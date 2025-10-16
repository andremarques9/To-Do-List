variable "table_name" {
  type        = string
  description = "Nome da tabela DynamoDB"
}

variable "partition_key" {
  type = object({
    name = string
    type = string
  })
  description = "Chave de partição"
}

variable "sort_key" {
  type = object({
    name = string
    type = string
  })
  description = "Chave de ordenação"
}

variable "billing_mode" {
  type        = string
  default     = "PAY_PER_REQUEST"
  description = "Modo de cobrança: PROVISIONED ou PAY_PER_REQUEST"
}

variable "global_secondary_indexes" {
  type = list(object({
    name            = string
    hash_key        = string
    range_key       = optional(string)
    projection_type = string
  }))
  default     = []
  description = "Configuração de GSIs para padrões de acesso adicionais"
}