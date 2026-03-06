---
layout: default
title: Asetukset ja datan muoto (Suomi)
---

Kieli: [English](../en/setup.md) | Suomi

# Asetukset ja datan muoto

Tämä ohje kertoo, miten kirjaudut sisään, järjestät Drive-kansion ja muotoilet Google Sheetin VocabQuizille.

## Sisällysluettelo
* TOC
{:toc}

## Edellytykset

- Google-tili
- Google Sheet, jossa sanastodata
- Internet-yhteys (tarvitaan datan lataamiseen)

## Kirjautuminen

Ensimmäisellä käynnistyskerralla kirjaudu Google-tilillä. Sovellus pyytää vain lukuoikeudet Sheetsiin ja Driveen.

![Kirjautumisnäkymä](../assets/img/feature-signin.png)

## Drive-kansion rakenne

Luo Driveen juuriin kansio nimeltä `VocabQuiz` ja tallenna sanastotaulukot tähän kansioon. Sovellus listaa taulukot tästä kansiosta.

![Drive ja Sheets](../assets/img/feature-drive-sheets.png)

## Taulukon valinta

Valitse asetuksista taulukko ja sheet-välilehti. Valinnat muistetaan.

![Asetukset](../assets/img/feature-settings.png)

## Sheetin muoto

Sovellus lukee sarakkeet A-D:

1. **A: Lähdekielen nimi** (esimerkiksi `English`)
2. **B: Kohdekielen nimi** (esimerkiksi `Finnish`)
3. **C: Lähdesana**
4. **D: Kohdesana**

Rivit, joista puuttuu arvoja tai joissa kielet ovat samat, ohitetaan.

### Esimerkki

| A (Lähdekieli) | B (Kohdekieli) | C (Lähdesana) | D (Kohdesana) |
|---|---|---|---|
| English | Finnish | cat | kissa |
| Finnish | English | koira | dog |

## Tuetut kielinimet

Käytä asetuksissa näkyviä tuettuja kielinimiä sarakkeissa A ja B. Sovellus tunnistaa myös yleisiä aliasmuotoja.

![Tuetut kielet](../assets/img/feature-localization.png)

## Harjoituspala (chunk)

Sovellus lataa sanat paloina (oletus 10) ja tarjoaa siirtymät palojen välillä päänäkymässä.
