# Description

The Hedera Ledger Pack integrates Hedera with Joget, and allows you to design Joget apps that interacts with the Hedera DLT.

Do see [documentation and sample app](https://dev.joget.org/community/display/marketplace/Hedera+Ledger+Pack).

> :warning: **This project is in early development stages and not ready for production use.**

# Changelog

## Q4 2022 (In Progress)
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

# Getting Help

JogetOSS is a community-led team for open source software related to the [Joget](https://www.joget.org) no-code/low-code application platform.
Projects under JogetOSS are community-driven and community-supported.
To obtain support, ask questions, get answers and help others, please participate in the [Community Q&A](https://answers.joget.org/).

# Contributing

This project welcomes contributions and suggestions, please open an issue or create a pull request.

Please note that all interactions fall under our [Code of Conduct](https://github.com/jogetoss/repo-template/blob/main/CODE_OF_CONDUCT.md).

# Licensing

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

NOTE: This software may depend on other packages that may be licensed under different open source licenses.
