# this script is part of the initialisation of the service

log_task_id() {
    if [ -n "$ECS_CONTAINER_METADATA_URI_V4" ]; then
        # Fetch ECS metadata and extract task ARN
        task_arn=$(curl -sS "$ECS_CONTAINER_METADATA_URI_V4" | grep -o '"com.amazonaws.ecs.task-arn":"[^"]*' | cut -d'"' -f4)

        # Extract task ID from task ARN
        task_id=$(echo "$task_arn" | grep -o '[^/]*$')

        if [ -z "$task_id" ]; then
            echo "ERROR: Failed to extract task ID from ECS metadata!"
            task_id="undefined"
        fi
    else
        task_id="undefined"
    fi

    echo "Task ID is $task_id"
}

log_task_id;

if [ -d /vault/secrets ] && [ -f /vault/secrets/service.env ]; then
    echo 'Sourcing secrets...'
    source /vault/secrets/service.env
fi

# Check for CORS_ORIGIN environment variable and add application argument
if [ -n "$CORS_ORIGIN" ]; then
    addApp "-cors.origin=$CORS_ORIGIN"
fi

# Check for CONF_FILE environment variable and add application argument
if [ -n "$CONF_FILE" ]; then
    addApp "-conf.file=$CONF_FILE"
fi
