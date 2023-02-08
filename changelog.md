# Changelog

## Q1 2023 (In Progress)
#### Added
- Modified: Changed default explorer to hashscan
- Modified: Used consistent naming for fungible and non-fungible tokens
- Added: Hedera Explorer Link Form Element
- Modified: Hedera Explorer Link - Improved value validation response time
- Added: Hedera Token Management Tool
- Modified: Hedera Token Management Tool - Support freeze/unfreeze transactions
- Modified: Hedera Token Management Tool - Support wipe & pause/unpause token transactions
- Modified: Hedera Token Management Tool - Support delete token transaction
- Modified: Hedera Token Management Tool - Support insert transaction memo
- Modified: Hedera Token Management Tool - Migrated burn token logic to this plugin
- Modified: Added helplinks for all plugins

#### Fixes
- Fixed: Hedera Explorer Link - Unable to find transaction despite valid tx ID
- Fixed: Hedera Token Management Tool - unnecessary target account property for pause & delete transactions

#### Deletions
- Removed: Hedera Associate Token Tool
- Removed: Hedera Burn Token Tool

## Q4 2022
#### Added
- Factorize calc token amount based on token decimals
- Factorize get and store form data
- Hedera Mint Token Tool - Support toggleable token permission keys
- Hedera Mint Token Tool - Support setting max token supply
- Hedera Mint Token Tool - Added toggleable token fee schedule key
- Hedera Account Load Binder - Support loading native token balances
- Added new Process Tool plugin - Hedera Burn Token Tool
- Hedera Send Transaction Tool - Support sending native tokens and NFTs
- Added new Process Tool plugin - Hedera Associate Token Tool
- Hedera Generate Account Tool - Support inserting custom account memo
- Hedera Generate Account Tool - Indication for Account ID as record ID
- Hedera Send Transaction Tool - Support inserting custom transaction memo

#### Fixes
- Hedera Mint Token Tool - Fixed mint more NFT cannot select IPFS CID property option
- Hedera Mint Token Tool - Fixed storing NFT data not unique record ID
- Fixed method throws incompatibility with Joget multi-tenant
- Fixed - DX8 - BackendUtil sometimes unable to parse hash variable for operator credentials
- Fixed: Misleading error log msg for ReceiptStatusException

## Q3 2022
#### Added
- Major codebase refactoring for plugin execution flow
- Added overridable input data validation method for all plugins
- Factorize generic plugin properties configs
- Hedera Send Transaction Tool - Simplify plugin configurations & add input data validation
- Factorize initializing Joget services
- Factorize storing generic tx data to workflow variables
- Added new Process Tool plugin - Hedera Mint Token Tool
- Hedera Mint Token Tool - Auto calculate initial supply based on defined token decimals
- Hedera Mint Token Tool - Added input validations
- Hedera Mint Token Tool - Auto calculate additional amount to mint based on token's set decimals

#### Fixes
- Major codebase restructuring & refactoring

## Q1 2022 (Initial commit)
#### Plugins introduced:
- Hedera Account Load Binder
- Hedera Generate Account Tool
- Hedera Scheduled Transaction Load Binder
- Hedera Send Transaction Tool
- Hedera Sign Scheduled Transaction Tool

#### Added
- Created initial documentation for plugin pack, along with first sample app as reference
