# Tokenization Service

Handles PAN (Primary Account Number) tokenization, tokenization vault operations, detokenization, and PCI DSS compliance.

## Features

- **PAN Tokenization**: Convert sensitive card data to tokens for secure storage
- **Token Mapping**: Maintain PAN-to-token mappings with encryption
- **Vault Operations**: Secure storage and retrieval of tokenized data
- **Detokenization**: Retrieve original card data (PCI compliant)
- **PCI DSS Compliance**: End-to-end encryption, audit trails, secure key management

## API Endpoints

- `POST /api/v1/tokens/create` - Create token from PAN
- `GET /api/v1/tokens/{token}` - Retrieve token info
- `POST /api/v1/detokenize` - Convert token back to PAN
- `DELETE /api/v1/tokens/{token}` - Delete token

## Database Schema

- `token_vault`: Stores tokenized card data
- `pan_token_mapping`: Maps PAN to tokens
- `tokenization_audit`: Audit trail for compliance
