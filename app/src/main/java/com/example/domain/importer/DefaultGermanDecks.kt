package com.example.domain.importer

import com.example.data.db.DeckEntity
import com.example.data.db.FlashcardEntity
import com.example.domain.german.GermanLanguageEngine

object DefaultGermanDecks {

    val starterDecks = listOf(
        DeckEntity(
            id = 1,
            name = "Goethe B1/B2 Core Vocabulary",
            description = "High-frequency German words for intermediate and advanced fluency with range selection.",
            colorHex = "#2563EB", // Sapphire
            cardCount = 85
        ),
        DeckEntity(
            id = 2,
            name = "Der Die Das Gender Mastery",
            description = "Master tricky German noun articles with clear color coding.",
            colorHex = "#059669", // Emerald
            cardCount = 35
        ),
        DeckEntity(
            id = 3,
            name = "German Verbs & Dynamic Synonyms",
            description = "Essential German verbs paired with dynamic synonym alternatives.",
            colorHex = "#7C3AED", // Violet
            cardCount = 30
        )
    )

    fun getCardsForDeck(deckId: Long): List<FlashcardEntity> {
        return when (deckId) {
            1L -> getGoetheB1Cards(1L)
            2L -> getGenderMasteryCards(2L)
            3L -> getVerbSynonymCards(3L)
            else -> emptyList()
        }
    }

    private fun getGoetheB1Cards(deckId: Long): List<FlashcardEntity> {
        val rawData = listOf(
            Triple("die Erfahrung, -en", "experience", "Er hat viel Erfahrung im Beruf."),
            Triple("die Entscheidung, -en", "decision", "Das war eine schwierige Entscheidung."),
            Triple("die Herausforderung, -en", "challenge", "Diese Aufgabe ist eine echte Herausforderung."),
            Triple("der Unterschied, -e", "difference", "Gibt es einen Unterschied zwischen den beiden?"),
            Triple("die Möglichkeit, -en", "opportunity / possibility", "Wir haben viele Möglichkeiten."),
            Triple("die Beziehung, -en", "relationship / connection", "Sie führen eine harmonische Beziehung."),
            Triple("das Verhalten", "behavior / conduct", "Sein Verhalten war vorbildlich."),
            Triple("die Verantwortung, -en", "responsibility", "Jeder trägt die Verantwortung für sein Handeln."),
            Triple("der Erfolg, -e", "success", "Ich wünsche dir viel Erfolg bei der Prüfung!"),
            Triple("die Entwicklung, -en", "development / progress", "Die wirtschaftliche Entwicklung ist stabil."),
            Triple("die Gesellschaft, -en", "society / company", "Unsere Gesellschaft verändert sich schnell."),
            Triple("die Zukunft", "future", "Niemand kann in die Zukunft schauen."),
            Triple("die Vergangenheit", "past", "Lerne aus der Vergangenheit."),
            Triple("die Gegenwart", "present", "Lebe in der Gegenwart."),
            Triple("die Bedingung, -en", "condition / terms", "Unter diesen Bedingungen stimme ich zu."),
            Triple("der Zusammenhang, -ä-e", "context / connection", "In diesem Zusammenhang ist das wichtig."),
            Triple("die Überraschung, -en", "surprise", "Das Geschenk war eine schöne Überraschung."),
            Triple("die Meinung, -en", "opinion", "Meiner Meinung nach ist das die beste Idee."),
            Triple("die Ursache, -n", "cause / reason", "Die Ursache des Fehlers ist bekannt."),
            Triple("die Wirkung, -en", "effect / impact", "Das Medikament hat eine schnelle Wirkung."),
            Triple("das Verständnis", "understanding / comprehension", "Vielen Dank für Ihr Verständnis."),
            Triple("der Einfluss, -ü-e", "influence", "Er hat großen Einfluss auf das Team."),
            Triple("die Eigenschaft, -en", "trait / characteristic", "Geduld ist eine wichtige Eigenschaft."),
            Triple("die Geduld", "patience", "Mit etwas Geduld schaffen wir das."),
            Triple("die Umwelt", "environment", "Wir müssen die Umwelt schützen."),
            Triple("der Vorschlag, -ä-e", "suggestion / proposal", "Ich habe einen interessanten Vorschlag."),
            Triple("das Ziel, -e", "goal / objective", "Wir haben unser Ziel erreicht."),
            Triple("die Fähigkeit, -en", "ability / skill", "Sie hat die Fähigkeit, Menschen zu motivieren."),
            Triple("die Kenntnis, -se", "knowledge / skill", "Gute Deutschkenntnisse sind erforderlich."),
            Triple("die Notwendigkeit", "necessity", "Es besteht die Notwendigkeit zu handeln."),
            Triple("die Gelegenheit, -en", "occasion / opportunity", "Nutzen Sie diese einmalige Gelegenheit!"),
            Triple("das Missverständnis, -se", "misunderstanding", "Es gab ein kleines Missverständnis."),
            Triple("die Bedeutung, -en", "significance / meaning", "Das Wort hat mehrere Bedeutungen."),
            Triple("die Nachfrage, -n", "demand (economic)", "Die Nachfrage nach Fachkräften steigt."),
            Triple("das Angebot, -e", "offer / supply", "Das ist ein faires Angebot."),
            Triple("die Vereinbarung, -en", "agreement", "Wir haben eine schriftliche Vereinbarung getroffen."),
            Triple("die Voraussetzung, -en", "prerequisite / condition", "Erfüllen Sie alle Voraussetzungen?"),
            Triple("die Lösung, -en", "solution", "Gemeinsam finden wir eine passende Lösung."),
            Triple("die Sicherheit, -en", "security / certainty", "Sicherheit steht an erster Stelle."),
            Triple("das Vertrauen", "trust / confidence", "Vertrauen ist die Basis jeder Freundschaft."),
            Triple("die Hoffnung, -en", "hope", "Die Hoffnung stirbt zuletzt."),
            Triple("die Wahrheit, -en", "truth", "Sag mir bitte die ganze Wahrheit."),
            Triple("die Lüge, -n", "lie / falsehood", "Lügen haben kurze Beine."),
            Triple("die Gewohnheit, -en", "habit", "Alte Gewohnheiten legt man schwer ab."),
            Triple("der Zweck, -e", "purpose", "Zu welchem Zweck dient dieses Formular?"),
            Triple("der Grund, -ü-e", "reason / ground", "Aus welchem Grund kommst du zu spät?"),
            Triple("die Schwierigkeit, -en", "difficulty", "Wir haben alle Schwierigkeiten gemeistert."),
            Triple("die Absicht, -en", "intention", "Ich hatte nicht die Absicht, dich zu verletzen."),
            Triple("der Fehler, -", "mistake / error", "Aus Fehlern lernt man."),
            // Cards 50 to 75 (Highlighted range micro-set demonstration)
            Triple("die Ausnahme, -n", "exception", "Keine Regel ohne Ausnahme."),
            Triple("die Belastung, -en", "burden / strain", "Der Stress ist eine große Belastung."),
            Triple("die Bestätigung, -en", "confirmation", "Sie erhalten eine Bestätigung per E-Mail."),
            Triple("die Beziehungskrise, -n", "relationship crisis", "Sie haben die Beziehungskrise überwunden."),
            Triple("der Blickwinkel, -", "perspective / viewpoint", "Aus diesem Blickwinkel wirkt alles anders."),
            Triple("die Chancengleichheit", "equal opportunity", "Chancengleichheit in der Bildung ist wichtig."),
            Triple("das Durchhaltevermögen", "perseverance / endurance", "Für Marathon braucht man Durchhaltevermögen."),
            Triple("das Einfühlungsvermögen", "empathy", "Gute Führungskräfte haben Einfühlungsvermögen."),
            Triple("die Einigkeit", "unity / agreement", "In der Gruppe herrschte große Einigkeit."),
            Triple("die Entschlossenheit", "determination", "Mit Entschlossenheit erreichte er sein Ziel."),
            Triple("die Fehleinschätzung, -en", "misjudgment", "Das war eine fatale Fehleinschätzung."),
            Triple("die Gelassenheit", "serenity / calmness", "Sie reagierte mit bewundernswerter Gelassenheit."),
            Triple("die Gerechtigkeit", "justice / fairness", "Gerechtigkeit ist ein hohes Gut."),
            Triple("die Hilfsbereitschaft", "helpfulness", "Vielen Dank für Ihre außergewöhnliche Hilfsbereitschaft!"),
            Triple("das Selbstbewusstsein", "self-confidence", "Das Training stärkt das Selbstbewusstsein."),
            Triple("die Sorgfalt", "diligence / care", "Präzision erfordert größte Sorgfalt."),
            Triple("die Überzeugung, -en", "conviction / belief", "Ich bin der festen Überzeugung, dass es klappt."),
            Triple("die Umsetzbarkeit", "feasibility", "Wir prüfen die Umsetzbarkeit des Projekts."),
            Triple("die Unabhängigkeit", "independence", "Finanzielle Unabhängigkeit ist ihr wichtig."),
            Triple("die Verbundenheit", "bond / solidarity", "Er spürt eine tiefe Verbundenheit zur Natur."),
            Triple("das Vorurteil, -e", "prejudice / bias", "Wir sollten Vorurteile abbauen."),
            Triple("die Weitsicht", "foresight / vision", "Ein Politiker mit echter Weitsicht."),
            Triple("die Wertschätzung", "appreciation / esteem", "Wertschätzung motiviert Mitarbeiter."),
            Triple("die Zuverlässigkeit", "reliability", "Auf seine Zuverlässigkeit kann man zählen."),
            Triple("die Zuvorkommenheit", "courtesy / attentiveness", "Der Kellner bediente mit Zuvorkommenheit."),
            // Cards 76+
            Triple("der Abstand, -ä-e", "distance / interval", "Bitte halten Sie ausreichend Abstand."),
            Triple("der Aufwand", "expenditure / effort", "Der Aufwand hat sich wirklich gelohnt."),
            Triple("der Beitrag, -ä-e", "contribution", "Er leistete einen wertvollen Beitrag."),
            Triple("der Durchschnitt, -e", "average", "Im Durchschnitt trainiert sie dreimal die Woche."),
            Triple("der Fortschritt, -e", "progress / advance", "Du machst große Fortschritte beim Deutschlernen."),
            Triple("der Rücktritt, -e", "resignation / cancellation", "Der Minister kündigte seinen Rücktritt an."),
            Triple("der Übergang, -ä-e", "transition / crossing", "Der Übergang zur neuen Software verlief reibungslos."),
            Triple("der Umfang", "scope / volume", "Der Umfang der Arbeit wurde unterschätzt."),
            Triple("der Zustand, -ä-e", "condition / state", "Das historische Haus ist in bestem Zustand."),
            Triple("die Zusammenfassung, -en", "summary", "Hier ist eine kurze Zusammenfassung.")
        )

        return rawData.mapIndexed { index, (front, back, example) ->
            val gender = GermanLanguageEngine.extractGender(front)
            val syns = GermanLanguageEngine.getSynonyms(front).joinToString(", ")
            FlashcardEntity(
                deckId = deckId,
                front = front,
                back = back,
                notes = example,
                gender = gender,
                partOfSpeech = "Noun",
                synonyms = syns,
                tags = "Goethe, B1, B2",
                exampleSentence = example,
                exampleTranslation = "",
                orderIndex = index + 1,
                state = 0
            )
        }
    }

    private fun getGenderMasteryCards(deckId: Long): List<FlashcardEntity> {
        val raw = listOf(
            // Masculine (der)
            Triple("der Löffel, -", "spoon", "masculine (der)"),
            Triple("der Teller, -", "plate", "masculine (der)"),
            Triple("der Teppich, -e", "carpet", "masculine (der)"),
            Triple("der Schlüssel, -", "key", "masculine (der)"),
            Triple("der Baum, -ä-e", "tree", "masculine (der)"),
            Triple("der Regen", "rain", "masculine (der)"),
            Triple("der Schmetterling, -e", "butterfly", "masculine (der)"),
            Triple("der Kühlschrank, -ä-e", "refrigerator", "masculine (der)"),
            Triple("der Flughafen, -ä-", "airport", "masculine (der)"),
            Triple("der Anzug, -ü-e", "suit", "masculine (der)"),
            // Feminine (die)
            Triple("die Gabel, -n", "fork", "feminine (die)"),
            Triple("die Wand, -ä-e", "wall", "feminine (die)"),
            Triple("die Tür, -en", "door", "feminine (die)"),
            Triple("die Brücke, -n", "bridge", "feminine (die)"),
            Triple("die Blume, -n", "flower", "feminine (die)"),
            Triple("die Wolke, -n", "cloud", "feminine (die)"),
            Triple("die Tasche, -n", "bag / pocket", "feminine (die)"),
            Triple("die Küche, -n", "kitchen", "feminine (die)"),
            Triple("die Zeitung, -en", "newspaper", "feminine (die)"),
            Triple("die Straße, -n", "street / road", "feminine (die)"),
            // Neuter (das)
            Triple("das Messer, -", "knife", "neuter (das)"),
            Triple("das Fenster, -", "window", "neuter (das)"),
            Triple("das Dach, -ä-er", "roof", "neuter (das)"),
            Triple("das Schloss, -ö-er", "castle / lock", "neuter (das)"),
            Triple("das Kissen, -", "pillow / cushion", "neuter (das)"),
            Triple("das Flugzeug, -e", "airplane", "neuter (das)"),
            Triple("das Gebäude, -", "building", "neuter (das)"),
            Triple("das Fahrrad, -ä-er", "bicycle", "neuter (das)"),
            Triple("das Werkzeug, -e", "tool", "neuter (das)"),
            Triple("das Buch, -ü-er", "book", "neuter (das)"),
            Triple("das Bett, -en", "bed", "neuter (das)"),
            Triple("das Geheimnis, -se", "secret", "neuter (das)"),
            Triple("das Erlebnis, -se", "adventure / experience", "neuter (das)"),
            Triple("das Ergebnis, -se", "result / outcome", "neuter (das)"),
            Triple("das Frühstück", "breakfast", "neuter (das)")
        )

        return raw.mapIndexed { index, (front, back, note) ->
            val gender = GermanLanguageEngine.extractGender(front)
            FlashcardEntity(
                deckId = deckId,
                front = front,
                back = back,
                notes = note,
                gender = gender,
                partOfSpeech = "Noun",
                tags = "Articles, Gender, $gender",
                orderIndex = index + 1,
                state = 0
            )
        }
    }

    private fun getVerbSynonymCards(deckId: Long): List<FlashcardEntity> {
        val verbs = listOf(
            Triple("beginnen", "to begin / start", "anfangen, starten, eröffnen"),
            Triple("anfangen", "to start / begin", "beginnen, einleiten, loslegen"),
            Triple("sprechen", "to speak / talk", "reden, sich unterhalten, sagen"),
            Triple("reden", "to talk / speak", "sprechen, plaudern, diskutieren"),
            Triple("sehen", "to see / look", "schauen, blicken, gucken, betrachten"),
            Triple("schauen", "to look / watch", "sehen, blicken, beobachten"),
            Triple("helfen", "to help / assist", "unterstützen, beistehen, entlasten"),
            Triple("unterstützen", "to support / aid", "helfen, fördern, befürworten"),
            Triple("verstehen", "to understand / comprehend", "begreifen, erfassen, kapieren"),
            Triple("begreifen", "to grasp / comprehend", "verstehen, nachvollziehen"),
            Triple("bekommen", "to receive / get", "erhalten, kriegen, empfangen"),
            Triple("erhalten", "to receive / maintain", "bekommen, empfangen, bewahren"),
            Triple("fragen", "to ask", "sich erkundigen, befragen, nachhaken"),
            Triple("antworten", "to answer / reply", "erwidern, entgegnen, reagieren"),
            Triple("zeigen", "to show / point out", "vorführen, weisen, präsentieren"),
            Triple("versuchen", "to try / attempt", "probieren, testen, erproben"),
            Triple("schaffen", "to manage / accomplish", "bewältigen, erreichen, vollenden"),
            Triple("nutzen", "to use / utilize", "verwenden, gebrauchen, einsetzen"),
            Triple("vermeiden", "to avoid / prevent", "umgehen, verhüten, ausweichen"),
            Triple("entscheiden", "to decide / determine", "beschließen, bestimmen, festlegen"),
            Triple("überzeugen", "to convince / persuade", "überreden, einleuchten"),
            Triple("verbessern", "to improve", "optimieren, steigern, verfeinern"),
            Triple("verändern", "to change / alter", "wandeln, modifizieren, umgestalten"),
            Triple("erklären", "to explain / declare", "erläutern, darlegen, verdeutlichen"),
            Triple("bedeuten", "to mean / signify", "heißen, besagen, implizieren"),
            Triple("erwarten", "to expect / await", "hoffen auf, voraussehen, antizipieren"),
            Triple("vergessen", "to forget", "nicht bedenken, versäumen"),
            Triple("erinnern", "to remind / remember", "ins Gedächtnis rufen, gedenken"),
            Triple("verpassen", "to miss (train/chance)", "versäumen, entgehen lassen"),
            Triple("erreichen", "to reach / attain", "ankommen, erzielen, erlangen")
        )

        return verbs.mapIndexed { index, (front, back, syns) ->
            FlashcardEntity(
                deckId = deckId,
                front = front,
                back = back,
                notes = "Synonyme: $syns",
                gender = "",
                partOfSpeech = "Verb",
                synonyms = syns,
                tags = "Verbs, B1, Synonyms",
                orderIndex = index + 1,
                state = 0
            )
        }
    }
}
