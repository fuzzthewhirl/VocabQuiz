---
layout: default
title: Features (English)
---

Language: English | [Suomi](../fi/features.md)

# Features

This page documents the core functionality of VocabQuiz. Replace image placeholders under `docs/assets/img/` with real screenshots.

## Table of Contents
* TOC
{:toc}

## Sign-in and permissions

VocabQuiz uses Google Sign-In and requires read-only access to Google Sheets and Google Drive to list and load vocabulary data.

![Sign-in screen](../assets/img/feature-signin.png)

## Quiz flow

The quiz presents a prompt, lets you tap to reveal the answer, and provides navigation for previous/next cards. Long-press copies the prompt or answer to clipboard.

![Main quiz screen](../assets/img/feature-main-quiz.png)

## Language pairs

Available language pairs are derived from the sheet data. The dropdown shows localized language names, while the internal data uses canonical names.

![Language pair selection](../assets/img/feature-language-pairs.png)

## Google Translate helper

The Translate button opens an in-app WebView dialog pointing to Google Translate, using the current word and detected source/target directions (reversed when the answer is revealed).

![Translate dialog](../assets/img/feature-translate.png)

## Settings and configuration

Settings let you:

- Select a spreadsheet from the Drive folder "VocabQuiz".
- Pick a sheet tab.
- Set the UI language.
- View supported language names for sheet columns A and B.
- Re-authenticate if permissions are missing.

![Settings screen](../assets/img/feature-settings.png)

## Localization

The UI is localized into multiple Western languages. The UI language can be set in Settings or left to system default.

![Localization options](../assets/img/feature-localization.png)

## Drive and Sheets integration

The app lists spreadsheets within a Drive folder named "VocabQuiz" and reads a sheet tab as the vocabulary source.

![Drive and Sheets flow](../assets/img/feature-drive-sheets.png)

## Error states and recovery

Errors are shown when data cannot be loaded (network, permissions, missing sheets). The settings screen provides status and re-auth actions.

![Error state](../assets/img/feature-errors.png)
