#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 2 ]; then
    echo "Usage: $0 <STACK_NAME> <TEMPLATE_PATH> [extra aws deploy args...]" >&2
    exit 2
fi

STACK="$1"; TEMPLATE="$2"; shift 2
EXTRA_ARGS=("$@")

export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-ap-south-1}"

get_status () {
    aws cloudformation describe-stacks --stack-name "$STACK" \
        --query "Stacks[0].StackStatus" --output text 2>/dev/null || echo "NO_STACK"
}

in_progress () {
    case "$1" in *_IN_PROGRESS) return 0 ;; *) return 1 ;; esac
}

wait_until_stable () {
    while true; do
        s="$(get_status)"
        if in_progress "$s"; then
            echo "Waiting for '$STACK' to stabilize (status=$s) ..."
            sleep 10
        else
            echo "'$STACK' stabilized: $s"
            break
        fi
    done
}

delete_if_unrecoverable () {
    s="$(get_status)"
    case "$s" in
        ROLLBACK_COMPLETE|UPDATE_ROLLBACK_COMPLETE)
            echo "Stack '$STACK' is in $s - deleting before redeploy..."
            aws cloudformation delete-stack --stack-name "$STACK"
            aws cloudformation wait stack-delete-complete --stack-name "$STACK"
            ;;
        ROLLBACK_IN_PROGRESS|UPDATE_ROLLBACK_IN_PROGRESS)
            wait_until_stable
            delete_if_unrecoverable
            ;;
    esac
}

safe_deploy () {
    set +e
    aws cloudformation deploy \
        --region "$AWS_DEFAULT_REGION" \
        --stack-name "$STACK" \
        --template-file "$TEMPLATE" \
        "${EXTRA_ARGS[@]}"
    rc=$?
    set -e

    s="$(get_status)"
    if [ $rc -ne 0 ]; then
        case "$s" in
            ROLLBACK_COMPLETE|UPDATE_ROLLBACK_COMPLETE)
                aws cloudformation delete-stack --stack-name "$STACK"
                aws cloudformation wait stack-delete-complete --stack-name "$STACK"
                exit 1 ;;
            *) exit $rc ;;
        esac
    fi
}

delete_if_unrecoverable
safe_deploy