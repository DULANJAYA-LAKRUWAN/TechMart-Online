#!/bin/bash
# =============================================================================
# TechMart Online — WildFly CLI Setup (Linux / macOS)
# Usage: $WILDFLY_HOME/bin/jboss-cli.sh --connect --file=scripts/setup-wildfly.cli
# =============================================================================

# PostgreSQL driver module
WILDFLY_HOME=${WILDFLY_HOME:-/opt/wildfly}
mkdir -p "$WILDFLY_HOME/modules/org/postgresql/main"
cp "$(find . -name 'postgresql-*.jar' -path '*/target/*')" "$WILDFLY_HOME/modules/org/postgresql/main/postgresql.jar"

cat > "$WILDFLY_HOME/modules/org/postgresql/main/module.xml" << 'MODULE'
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.postgresql">
    <resources><resource-root path="postgresql.jar"/></resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
    </dependencies>
</module>
MODULE

echo "PostgreSQL driver module installed."

# Run CLI setup
$WILDFLY_HOME/bin/jboss-cli.sh --connect --file=scripts/setup-wildfly.cli
echo "WildFly configured."
