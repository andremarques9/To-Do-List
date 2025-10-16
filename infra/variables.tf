variable "aws_region" {
  type        = string
  description = "Região AWS para todos os recursos"
  default     = "us-east-2"
}

variable "function_name" {
  type        = string
  description = "Nome da função Lambda Java"
}

variable "handler" {
  type        = string
  description = "Classe e método handler (ex: com.exemplo.Handler::handleRequest)"
}

variable "role_arn" {
  type        = string
  description = "ARN da IAM Role para execução da Lambda"
}

variable "jar_file_path" {
  type        = string
  description = "Caminho local do JAR empacotado da Lambda"
}

variable "dynamodb_table_name" {
  type        = string
  description = "Nome da tabela DynamoDB (Single Table Design)"
}

variable "dynamodb_partition_key" {
  type = object({
    name = string
    type = string
  })
  description = "Chave de partição da tabela"
}

variable "dynamodb_sort_key" {
  type = object({
    name = string
    type = string
  })
  description = "Chave de ordenação da tabela"
}

variable "dynamodb_billing_mode" {
  type        = string
  description = "Modo de cobrança DynamoDB: PROVISIONED ou PAY_PER_REQUEST"
  default     = "PAY_PER_REQUEST"
}

variable "dynamodb_global_secondary_indexes" {
  type = list(object({
    name            = string
    hash_key        = string
    range_key       = optional(string)
    projection_type = string
  }))
  description = "Configuração de GSIs adicionais"
  default     = []
}