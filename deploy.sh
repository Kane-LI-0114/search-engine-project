#!/usr/bin/env bash
# =============================================================================
# deploy.sh — Search Engine full deployment script
# Usage:
#   ./deploy.sh            # Interactive menu
#   ./deploy.sh docker     # Docker deployment (non-interactive)
#   ./deploy.sh tomcat     # Tomcat deployment (non-interactive)
#   ./deploy.sh crawl      # Run crawler only
#   ./deploy.sh build      # Build WAR only
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Colors
# ---------------------------------------------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

info()    { echo -e "${CYAN}[INFO]${RESET}  $*"; }
success() { echo -e "${GREEN}[OK]${RESET}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
error()   { echo -e "${RED}[ERROR]${RESET} $*" >&2; }
step()    { echo -e "\n${BOLD}▶ $*${RESET}"; }
die()     { error "$*"; exit 1; }

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

WAR="target/search-engine.war"
DB_PATTERN="searchengine_db.*"
STOPWORDS="src/main/resources/stopwords.txt"

# ---------------------------------------------------------------------------
# Step 0 — Check prerequisites
# ---------------------------------------------------------------------------
check_prerequisites() {
    step "Checking prerequisites"

    # Java
    if command -v java &>/dev/null; then
        JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        success "Java found: $JAVA_VER"
    else
        die "Java not found. Install JDK 8 or higher."
    fi

    # Maven
    if command -v mvn &>/dev/null; then
        MVN_VER=$(mvn -v 2>&1 | head -1)
        success "Maven found: $MVN_VER"
    else
        die "Maven not found. Install Apache Maven 3.6 or higher."
    fi
}

# ---------------------------------------------------------------------------
# Step 1 — Install local jars into Maven local repo (if needed)
# ---------------------------------------------------------------------------
install_local_deps() {
    step "Checking local Maven dependencies"

    local HTMLPARSER_INSTALLED HTMLLEXER_INSTALLED JDBM_INSTALLED
    HTMLPARSER_INSTALLED=$(mvn dependency:get \
        -Dartifact=org.htmlparser:htmlparser:2.1 -q 2>/dev/null && echo yes || echo no)
    HTMLLEXER_INSTALLED=$(mvn dependency:get \
        -Dartifact=org.htmlparser:htmllexer:2.1 -q 2>/dev/null && echo yes || echo no)
    JDBM_INSTALLED=$(mvn dependency:get \
        -Dartifact=jdbm:jdbm:1.0 -q 2>/dev/null && echo yes || echo no)

    if [[ "$HTMLPARSER_INSTALLED" == "yes" && "$HTMLLEXER_INSTALLED" == "yes" && "$JDBM_INSTALLED" == "yes" ]]; then
        success "All local dependencies already installed."
        return 0
    fi

    warn "Some local jars are missing from Maven repo. Checking for jar files..."

    # htmlparser
    if [[ "$HTMLPARSER_INSTALLED" != "yes" ]]; then
        if [[ -f "htmlparser.jar" ]]; then
            mvn install:install-file \
                -Dfile=htmlparser.jar \
                -DgroupId=org.htmlparser -DartifactId=htmlparser \
                -Dversion=2.1 -Dpackaging=jar -q
            success "Installed htmlparser.jar"
        else
            die "htmlparser.jar not found in project root. Download from http://htmlparser.sourceforge.net/ and place it here."
        fi
    fi

    # htmllexer
    if [[ "$HTMLLEXER_INSTALLED" != "yes" ]]; then
        if [[ -f "htmllexer.jar" ]]; then
            mvn install:install-file \
                -Dfile=htmllexer.jar \
                -DgroupId=org.htmlparser -DartifactId=htmllexer \
                -Dversion=2.1 -Dpackaging=jar -q
            success "Installed htmllexer.jar"
        else
            die "htmllexer.jar not found in project root."
        fi
    fi

    # jdbm
    if [[ "$JDBM_INSTALLED" != "yes" ]]; then
        if [[ -f "jdbm-1.0.jar" ]]; then
            mvn install:install-file \
                -Dfile=jdbm-1.0.jar \
                -DgroupId=jdbm -DartifactId=jdbm \
                -Dversion=1.0 -Dpackaging=jar -q
            success "Installed jdbm-1.0.jar"
        else
            die "jdbm-1.0.jar not found in project root."
        fi
    fi
}

# ---------------------------------------------------------------------------
# Step 2 — Run crawler (skip if db files already exist)
# ---------------------------------------------------------------------------
run_crawler() {
    step "Crawler"

    if ls $DB_PATTERN &>/dev/null 2>&1; then
        info "Database files found. Re-crawling to update pages..."
    else
        info "No database files found. Starting fresh crawl..."
    fi

    info "Crawling (BFS, up to 300 pages)..."
    info "This typically takes ~8-10 seconds."
    mvn clean compile -q
    mvn exec:java -Dexec.mainClass="search.crawler.Spider"
    success "Crawl complete. Database files updated."
}

# ---------------------------------------------------------------------------
# Step 3 — Build WAR
# ---------------------------------------------------------------------------
build_war() {
    step "Building WAR"
    mvn clean package -DskipTests -q
    if [[ -f "$WAR" ]]; then
        success "WAR built: $WAR"
    else
        die "WAR not found after build."
    fi
}

# ---------------------------------------------------------------------------
# Step 4A — Docker deployment
# ---------------------------------------------------------------------------
deploy_docker() {
    step "Docker deployment"

    if ! command -v docker &>/dev/null; then
        die "Docker not found. Install Docker Desktop first."
    fi

    if ! docker info &>/dev/null 2>&1; then
        die "Docker daemon is not running. Start Docker Desktop first."
    fi

    if ! ls $DB_PATTERN &>/dev/null 2>&1; then
        die "Database files (searchengine_db.*) not found. Run the crawler first: $0 crawl"
    fi

    # Stop and remove existing container before doing anything else
    if docker ps -q --filter "name=search-engine" | grep -q .; then
        warn "Stopping existing search-engine container..."
        docker stop search-engine &>/dev/null
        docker rm search-engine &>/dev/null
    elif docker ps -aq --filter "name=search-engine" | grep -q .; then
        docker rm search-engine &>/dev/null
    fi

    # Detect host platform
    local ARCH
    ARCH=$(uname -m)
    local HOST_PLATFORM
    case "$ARCH" in
        arm64|aarch64) HOST_PLATFORM="linux/arm64" ;;
        *)             HOST_PLATFORM="linux/amd64"  ;;
    esac

    # Resolve base image — fall back to mirror if Docker Hub is unreachable
    # Mirror only has amd64, so platform must follow the image, not the host
    local PRIMARY_IMAGE="tomcat:9.0-jdk17-openjdk"
    local MIRROR_IMAGE="swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/tomcat:9-jdk17-openjdk-slim"
    local BASE_IMAGE PLATFORM

    info "Checking Docker Hub connectivity..."
    if curl -sf --max-time 5 https://registry-1.docker.io/v2/ -o /dev/null 2>&1; then
        success "Docker Hub reachable — using $PRIMARY_IMAGE ($HOST_PLATFORM)"
        BASE_IMAGE="$PRIMARY_IMAGE"
        PLATFORM="$HOST_PLATFORM"
    else
        warn "Docker Hub unreachable — switching to mirror: $MIRROR_IMAGE (linux/amd64)"
        BASE_IMAGE="$MIRROR_IMAGE"
        PLATFORM="linux/amd64"   # mirror only provides amd64
    fi

    info "Building Docker image..."
    docker build --platform "$PLATFORM" --build-arg BASE_IMAGE="$BASE_IMAGE" -t search-engine .
    success "Docker image built: search-engine"

    info "Starting container..."
    docker run -d --name search-engine --platform "$PLATFORM" -p 8080:8080 search-engine
    success "Container started."
    echo -e "\n${GREEN}${BOLD}App running at: http://localhost:8080/${RESET}"
    echo -e "Container logs: ${CYAN}docker logs -f search-engine${RESET}"
    echo -e "Stop:           ${CYAN}docker stop search-engine${RESET}"
}

# ---------------------------------------------------------------------------
# Step 4B — Tomcat deployment
# ---------------------------------------------------------------------------
deploy_tomcat() {
    step "Tomcat deployment"

    if [[ -z "${CATALINA_HOME:-}" ]]; then
        die "CATALINA_HOME is not set. Export it first: export CATALINA_HOME=/path/to/tomcat"
    fi

    if [[ ! -d "$CATALINA_HOME" ]]; then
        die "CATALINA_HOME=$CATALINA_HOME does not exist."
    fi

    if ! ls $DB_PATTERN &>/dev/null 2>&1; then
        die "Database files (searchengine_db.*) not found. Run the crawler first: $0 crawl"
    fi

    info "Copying WAR to Tomcat webapps..."
    cp "$WAR" "$CATALINA_HOME/webapps/"
    success "Copied $WAR → $CATALINA_HOME/webapps/"

    info "Copying database files to Tomcat bin/..."
    cp $DB_PATTERN "$CATALINA_HOME/bin/"
    success "Copied searchengine_db.* → $CATALINA_HOME/bin/"

    info "Copying stopwords.txt..."
    cp "$STOPWORDS" "$CATALINA_HOME/bin/"
    success "Copied stopwords.txt → $CATALINA_HOME/bin/"

    info "Starting Tomcat..."
    if [[ -f "$CATALINA_HOME/bin/startup.sh" ]]; then
        "$CATALINA_HOME/bin/startup.sh"
    else
        die "startup.sh not found in $CATALINA_HOME/bin/"
    fi

    success "Tomcat started."
    echo -e "\n${GREEN}${BOLD}App running at: http://localhost:8080/search-engine/${RESET}"
    echo -e "Stop Tomcat: ${CYAN}\$CATALINA_HOME/bin/shutdown.sh${RESET}"
}

# ---------------------------------------------------------------------------
# Interactive menu
# ---------------------------------------------------------------------------
show_menu() {
    echo -e "\n${BOLD}=============================="
    echo -e " Search Engine Deployment"
    echo -e "==============================${RESET}"
    echo "  1) Full deploy — Docker   (crawl if needed + build + docker run)"
    echo "  2) Full deploy — Tomcat   (crawl if needed + build + copy to Tomcat)"
    echo "  3) Run crawler only"
    echo "  4) Build WAR only"
    echo "  5) Stop Docker container"
    echo "  6) Exit"
    echo ""
    read -rp "Choose [1-6]: " CHOICE

    case "$CHOICE" in
        1) MODE="docker" ;;
        2) MODE="tomcat" ;;
        3) MODE="crawl"  ;;
        4) MODE="build"  ;;
        5) MODE="stop"   ;;
        6) exit 0        ;;
        *) die "Invalid choice." ;;
    esac
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
MODE="${1:-}"

if [[ -z "$MODE" ]]; then
    show_menu
fi

check_prerequisites

case "$MODE" in
    docker)
        install_local_deps
        run_crawler
        build_war
        deploy_docker
        ;;
    tomcat)
        install_local_deps
        run_crawler
        build_war
        deploy_tomcat
        ;;
    crawl)
        install_local_deps
        run_crawler
        ;;
    build)
        install_local_deps
        build_war
        ;;
    stop)
        step "Stopping Docker container"
        if docker ps -q --filter "name=search-engine" | grep -q .; then
            docker stop search-engine &>/dev/null
            docker rm search-engine &>/dev/null
            success "Container stopped and removed."
        elif docker ps -aq --filter "name=search-engine" | grep -q .; then
            docker rm search-engine &>/dev/null
            success "Stopped container removed."
        else
            warn "No search-engine container found."
        fi
        ;;
    *)
        die "Unknown mode: $MODE. Use: docker | tomcat | crawl | build | stop"
        ;;
esac
