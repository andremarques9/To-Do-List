resource "aws_cognito_user_pool_client" "userpool_client" {
  name                                 = "client"
  user_pool_id                         = aws_cognito_user_pool.pool.id
  callback_urls                        = ["https://example.com"]
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code", "implicit"]
  allowed_oauth_scopes                 = ["email", "openid"]
  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH",
    "ALLOW_USER_AUTH"
  ]

  supported_identity_providers         = ["COGNITO"]
}

resource "aws_cognito_user_pool" "pool" {
  name = "pool"
}