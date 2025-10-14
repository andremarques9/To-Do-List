output "api_id" {
  value = aws_api_gateway_rest_api.this.id
}

output "api_url" {
  value = "https://${aws_api_gateway_rest_api.this.id}.execute-api.${var.aws_region}.amazonaws.com/${var.stage_name}"
}

output "execution_arn" {
  value       = aws_api_gateway_rest_api.this.execution_arn
  description = "ARN para usar em Lambda permissions"
}