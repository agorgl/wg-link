#!/bin/bash

# Constants
ROUTING_TABLE=5050
SYSTEMD_UNIT_DIR=/etc/systemd/system

read -r -d '' ROUTES_SERVICE_UNIT << EOM
[Unit]
Description=Synchronize container routes
Wants=network.target
After=network-online.target

[Service]
Type=oneshot
ExecStart=wg-routes.sh
EOM

read -r -d '' ROUTES_TIMER_UNIT << EOM
[Unit]
Description=Periodic route synchronization

[Timer]
OnCalendar=*:*:00/30
AccuracySec=1sec
Persistent=true

[Install]
WantedBy=timers.target
EOM

# Arguments
PERSIST=false
REMOVE=false
while [ "$1" != "" ]; do
    case $1 in
        -p|--persist)
            PERSIST=true
            ;;
        -r|--remove)
            REMOVE=true
            ;;
    esac
    shift
done

# Gather applicable containers
containers=$(podman ps --format '{{ .Names }}' | grep 'wg-')

# For each container
all_routes=""
for container in $containers; do
    # Get container ip and routes
    container_ip=$(podman inspect $container -f '{{ .NetworkSettings.IPAddress }}')
    container_routes=$(podman exec $container ip route | grep wg | awk '{print $1}' | grep '/' | xargs -r printf "%s:$container_ip\n")
    all_routes+=$container_routes

    # Get existing routes
    routes=$(ip route show table $ROUTING_TABLE | awk '{print $1 ":" $3}')

    # Calculate new routes
    new_routes=$(comm -23 <(printf "%s\n" $container_routes | sort) <(printf "%s\n" $routes | sort))

    # Add them to custom routing table
    for r in $new_routes; do
        route=${r/:/ via }
        echo "Adding new route $route"
        ip route add $route table $ROUTING_TABLE
    done
done

# Calculate old routes
routes=$(ip route show table $ROUTING_TABLE | awk '{print $1 ":" $3}')
old_routes=$(comm -13 <(printf "%s\n" $all_routes | sort | uniq) <(printf "%s\n" $routes | sort))

# Remove old routes from custom routing table
for r in $old_routes; do
    route=${r/:/ via }
    echo "Removing old route $route"
    ip route del $route table $ROUTING_TABLE
done

# Add rule to custom routing table
if [ -z "$(ip rule list | awk '/lookup/ {print $NF}' | grep $ROUTING_TABLE)" ]; then
    echo "Adding table $ROUTING_TABLE to routing policy database"
    ip rule add from all table $ROUTING_TABLE
fi

# Persist and re-run self through systemd timer
if [ $PERSIST = true ]; then
    # Copy self
    echo "Copying self to /usr/local/bin"
    cp -v "$0" /usr/local/bin/

    # Generate service and timer unit files
    echo "Generating systemd unit files"
    [ ! -f $SYSTEMD_UNIT_DIR/wg-routes.service ] && echo "$ROUTES_SERVICE_UNIT" > $SYSTEMD_UNIT_DIR/wg-routes.service
    [ ! -f $SYSTEMD_UNIT_DIR/wg-routes.timer ] && echo "$ROUTES_TIMER_UNIT" > $SYSTEMD_UNIT_DIR/wg-routes.timer

    # Enable and start timer
    echo "Enabling and starting wg-routes.timer"
    systemctl daemon-reload
    systemctl enable --now wg-routes.timer
fi

# Remove self persistence
if [ $REMOVE = true ]; then
    # Remove unit files
    units=$(systemctl list-unit-files --all | grep 'wg-routes' | awk '{print $1}')
    for unit in $units; do
        echo "Disabling and stopping $unit"
        systemctl disable --now $unit

        echo "Removing $unit"
        unit_path=$(systemctl show -P FragmentPath $unit)
        rm $unit_path
    done
    systemctl daemon-reload

    # Remove self
    echo "Removing self from /usr/local/bin"
    rm "$0"
fi
