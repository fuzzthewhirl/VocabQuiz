---
layout: default
title: Toiminnot (Suomi)
---

Kieli: [English](../en/features.md) | Suomi

# Toiminnot

Tällä sivulla kuvataan VocabQuizin keskeiset toiminnot. Korvaa `docs/assets/img/`-kansion paikkamerkkikuvat oikeilla kuvakaappauksilla.

## Sisällysluettelo
* TOC
{:toc}

## Kirjautuminen ja oikeudet

VocabQuiz käyttää Google-kirjautumista ja tarvitsee lukuoikeudet Google Sheetsiä ja Google Drivea varten.

![Kirjautumisnäkymä](../assets/img/feature-signin.png)

## Kysely/harjoitusnäkymä

Päänäkymässä näet sanan, paljastat vastauksen napauttamalla ja siirryt edelliseen tai seuraavaan korttiin. Pitkä painallus kopioi tekstin leikepöydälle.

![Päänäkymä](../assets/img/feature-main-quiz.png)

## Kieliparit

Kieliparit muodostuvat taulukon datasta. Pudotusvalikossa näkyvät lokalisoidut kielien nimet, mutta sisäinen data käyttää kanonisia nimiä.

![Kieliparin valinta](../assets/img/feature-language-pairs.png)

## Google Translate

Käännöspainike avaa sovelluksen sisäisen WebView-ikkunan Google Translateen. Lähde- ja kohdekieli valitaan nykyisen tilan mukaan.

![Google Translate -ikkuna](../assets/img/feature-translate.png)

## Asetukset ja määritykset

Asetuksista voit:

- Valita taulukon Drive-kansiosta "VocabQuiz".
- Valita sheetin välilehden.
- Asettaa käyttöliittymän kielen.
- Nähdä tuetut kielinimet sarakkeita A ja B varten.
- Uudelleentunnistautua, jos oikeudet puuttuvat.

![Asetukset](../assets/img/feature-settings.png)

## Lokalisaatio

Käyttöliittymä on lokalisoitu useille länsimaisille kielille. Kielen voi asettaa asetuksista tai käyttää järjestelmän oletusta.

![Lokalisaatiot](../assets/img/feature-localization.png)

## Drive- ja Sheets-integraatio

Sovellus listaa taulukot Drive-kansiosta "VocabQuiz" ja lukee valitun sheetin datan.

![Drive ja Sheets](../assets/img/feature-drive-sheets.png)

## Virhetilanteet ja palautuminen

Virhetilanteet näkyvät esimerkiksi verkon puuttuessa, oikeuksien puuttuessa tai kun dataa ei löydy. Asetukset-näytössä on näkyvissä tilat ja uudelleenkirjautuminen.

![Virhetila](../assets/img/feature-errors.png)
