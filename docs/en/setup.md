---
layout: default
title: Setup and Data Format (English)
---

Language: English | [Suomi](../fi/setup.md)

# Setup and Data Format

This guide explains how to sign in, organize your Drive folder, and format your Google Sheet for VocabQuiz.

## Table of Contents
* TOC
{:toc}

## Prerequisites

- A Google account
- A Google Sheet containing vocabulary data
- Internet access (required for loading sheets)

## Sign in

On first launch, sign in with Google. The app requests read-only permissions for Sheets and Drive.

![Sign-in screen](../assets/img/feature-signin.png)

## Drive folder structure

Create a Drive folder named exactly `VocabQuiz` in your Drive root. Place your vocabulary spreadsheets inside this folder. The app lists spreadsheets from this folder.

![Drive and Sheets flow](../assets/img/feature-drive-sheets.png)

## Spreadsheet selection

Use Settings to pick the spreadsheet and sheet tab. The selection is remembered.

![Settings screen](../assets/img/feature-settings.png)

## Sheet format

The app reads columns A through D:

1. **A: Source language name** (for example `English`)
2. **B: Target language name** (for example `Finnish`)
3. **C: Source word**
4. **D: Target word**

Rows with missing values or same-language pairs are ignored.

### Example

| A (Source language) | B (Target language) | C (Source word) | D (Target word) |
|---|---|---|---|
| English | Finnish | cat | kissa |
| Finnish | English | koira | dog |

## Supported language names

Use the supported language names listed in Settings. The app normalizes common aliases (for example `espanol` -> `Spanish`).

![Supported languages](../assets/img/feature-localization.png)

## Quiz chunking

VocabQuiz loads words in chunks (default size 10) and provides navigation between chunks in the main quiz screen.
