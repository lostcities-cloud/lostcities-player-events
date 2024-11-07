variable "version" {
  type    = string
  default = "latest"
}

variable "cpu" {
  type    = number
  default = 500
}

variable "memory" {
  type    = number
  default = 500
}

variable "memory_max" {
  type    = number
  default = 1028
}

variable "memory_reservation_mb" {
  type    = number
  default = 256
}

variable "memory_swap_mb" {
  type = number
  default = 512
}

variable "memory_swappiness" {
  type = number
  default = 0
}

variable count {
  type    = number
  default = 1
}

variable max_parallel {
  type    = number
  default = 2
}

job "player-events" {
  region    = "global"
  namespace = "lostcities"
  datacenters = ["tower-datacenter"]

  spread {
    attribute = "${node.datacenter}"
    weight    = 100
  }

  group "player-events" {
    count = var.count

    restart {
      attempts = 10
      interval = "5m"
      delay    = "25s"
      mode     = "delay"
    }

    network {
      mode = "bridge"

      port "management-port" {
        to = 4452
      }

      port "service-port" {
        to = 8093
      }
    }

    service {
      name = "player-events-service"
      port = "service-port"
      tags = ["urlprefix-/api/player-events"]
      #address_mode = "alloc"

      check {
        type                     = "http"
        port                     = "management-port"
        path                     = "/management/player-events/actuator/health"
        interval                 = "30s"
        timeout                  = "10s"
        failures_before_critical = 20
        failures_before_warning  = 10
      }
    }

    service {
      name = "player-events-management"
      port = "management-port"
      tags = ["urlprefix-/management/player-events/actuator"]
      #address_mode = "alloc"

      check {
        type                     = "http"
        port                     = "management-port"
        path                     = "/management/player-events/actuator/health"
        interval                 = "30s"
        timeout                  = "10s"
        failures_before_critical = 20
        failures_before_warning  = 10
      }
    }

    task "player-events" {
      driver = "podman"

      env {
        SPRING_PROFILES_ACTIVE = "dev"
      }

      resources {
        cpu      = var.cpu
        memory     = var.memory
        memory_max = var.memory_max
      }

      config {
        force_pull = true
        image      = "ghcr.io/lostcities-cloud/lostcities-player-events:${var.version}"
        #memory_swap = "${var.memory_swap_mb}m"
        #memory_swappiness = var.memory_swappiness
        memory_reservation = "${var.memory_reservation_mb}m"


        ports = ["service-port", "management-port"]
        logging {
          driver = "journald"
          options = [
            {
              "job" = "lostcities-player-events"
            }
          ]

        }
      }

      template {
        data        = <<EOF
{{ range service "postgres" }}
POSTGRES_IP="{{ .Address }}"
{{ else }}
{{ end }}
{{ range service "redis" }}
REDIS_IP="{{ .Address }}"
{{ else }}
{{ end }}
{{ range service "rabbitmq" }}
RABBITMQ_IP="{{ .Address }}"
{{ else }}
{{ end }}
EOF
        change_mode = "restart"
        destination = "local/discovery.env"
        env         = true
      }
    }

    update {
      max_parallel     = var.max_parallel
      min_healthy_time = "20s"
      healthy_deadline = "3m"
      auto_revert      = true
      canary           = 1
      auto_promote     = true
    }
  }
}
