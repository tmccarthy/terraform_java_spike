variable "display_zip_path" {}

variable "bucket_name" {
  default = "bucket1.tmm.id.au"
}

variable "object_name" {
  default = "object"
}

provider "aws" {
  region = "ap-southeast-2"
}

data "aws_region" "current" {}

resource "aws_s3_bucket" "bucket_1" {
  bucket = "${var.bucket_name}"
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::${var.bucket_name}/${var.object_name}",
      "Principal": {
        "AWS": [
          "${aws_iam_role.iam_for_lambda.arn}"
        ]
      }
    }
  ]
}
EOF
}

resource "aws_s3_bucket_object" "object" {
  bucket  = "${aws_s3_bucket.bucket_1.id}"
  key     = "${var.object_name}"
  content = "0"
}

resource "aws_iam_role" "iam_for_lambda" {
  name = "iam_for_lambda"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_lambda_function" "display" {
  filename         = "${var.display_zip_path}"
  function_name    = "display"
  role             = "${aws_iam_role.iam_for_lambda.arn}"
  handler          = "au.id.tmm.terraformjavaspike.lambda.display.Lambda"
  source_code_hash = "${base64sha256(file(var.display_zip_path))}"
  runtime          = "java8"
  timeout          = 30
  memory_size      = 256

  environment {
    variables = {
      BUCKET_NAME = "${aws_s3_bucket.bucket_1.id}"
    }
  }
}

resource "aws_api_gateway_rest_api" "api" {
  name = "terraform_java_spike"
}

resource "aws_api_gateway_deployment" "deployment" {
  depends_on = ["aws_api_gateway_integration.integration"]
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  stage_name = "prod"
}

resource "aws_api_gateway_method_settings" "aws_api_gateway_method_settings" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  stage_name  = "${aws_api_gateway_deployment.deployment.stage_name}"
  method_path = "${aws_api_gateway_resource.resource.path_part}/${aws_api_gateway_method.method.http_method}"

  settings {
    metrics_enabled = false
  }
}

resource "aws_api_gateway_resource" "resource" {
  path_part = "display"
  parent_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
}

resource "aws_api_gateway_method" "method" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.resource.id}"
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "integration" {
  rest_api_id             = "${aws_api_gateway_rest_api.api.id}"
  resource_id             = "${aws_api_gateway_resource.resource.id}"
  http_method             = "${aws_api_gateway_method.method.http_method}"
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${data.aws_region.current.name}:lambda:path/2015-03-31/functions/${aws_lambda_function.display.arn}/invocations"

  request_templates {
    "application/json" = ""
  }
}

resource "aws_lambda_permission" "display" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.display.arn}"
  principal     = "apigateway.amazonaws.com"

  # More: http://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-control-access-using-iam-policies-to-invoke-api.html
  source_arn = "${aws_api_gateway_rest_api.api.execution_arn}/*/*/*"
}

output "display_url" {
  value = "${aws_api_gateway_deployment.deployment.invoke_url}${aws_api_gateway_resource.resource.path}"
}
