package de.menkalian.draco.data.quesstimate

import kotlinx.serialization.Serializable

@Serializable
enum class Category {
    GENERAL_KNOWLEDGE,
    ENTERTAINMENT_BOOKS,
    ENTERTAINMENT_FILM,
    ENTERTAINMENT_MUSIC,
    ENTERTAINMENT_MUSICALS_THEATRES,
    ENTERTAINMENT_TELEVISION,
    ENTERTAINMENT_VIDEO_GAMES,
    ENTERTAINMENT_BOARD_GAMES,
    ENTERTAINMENT_COMICS,
    ENTERTAINMENT_JAPANESE_ANIME_MANGA,
    ENTERTAINMENT_CARTOON_ANIMATIONS,
    SCIENCE_AND_NATURE,
    SCIENCE_COMPUTERS,
    SCIENCE_MATHEMATICS,
    SCIENCE_GADGETS,
    MYTHOLOGY,
    SPORTS,
    GEOGRAPHY,
    HISTORY,
    POLITICS,
    ART,
    CELEBRITIES,
    ANIMALS,
    VEHICLES;

    fun toDisplayString(language: Language = Language.ENGLISH): String {
        when (language) {
            Language.ENGLISH -> {
                return when (this) {
                    GENERAL_KNOWLEDGE                  -> "General Knowledge"
                    ENTERTAINMENT_BOOKS                -> "Entertainment: Books"
                    ENTERTAINMENT_FILM                 -> "Entertainment: Film"
                    ENTERTAINMENT_MUSIC                -> "Entertainment: Music"
                    ENTERTAINMENT_MUSICALS_THEATRES    -> "Entertainment: Musicals & Theatres"
                    ENTERTAINMENT_TELEVISION           -> "Entertainment: Television"
                    ENTERTAINMENT_VIDEO_GAMES          -> "Entertainment: Video Games"
                    ENTERTAINMENT_BOARD_GAMES          -> "Entertainment: Board Games"
                    ENTERTAINMENT_COMICS               -> "Entertainment: Comics"
                    ENTERTAINMENT_JAPANESE_ANIME_MANGA -> "Entertainment: Japanese Anime & Manga"
                    ENTERTAINMENT_CARTOON_ANIMATIONS   -> "Entertainment: Cartoon & Animations"
                    SCIENCE_AND_NATURE                 -> "Science & Nature"
                    SCIENCE_COMPUTERS                  -> "Science: Computers"
                    SCIENCE_MATHEMATICS                -> "Science: Mathematics"
                    SCIENCE_GADGETS                    -> "Science: Gadgets"
                    MYTHOLOGY                          -> "Mythology"
                    SPORTS                             -> "Sports"
                    GEOGRAPHY                          -> "Geography"
                    HISTORY                            -> "History"
                    POLITICS                           -> "Politics"
                    ART                                -> "Art"
                    CELEBRITIES                        -> "Celebrities"
                    ANIMALS                            -> "Animals"
                    VEHICLES                           -> "Vehicles"
                }
            }
            Language.GERMAN  -> {
                return when (this) {
                    GENERAL_KNOWLEDGE                  -> "Allgemeinwissen"
                    ENTERTAINMENT_BOOKS                -> "Bücher"
                    ENTERTAINMENT_FILM                 -> "Filme"
                    ENTERTAINMENT_MUSIC                -> "Musik"
                    ENTERTAINMENT_MUSICALS_THEATRES    -> "Musicals/Theater"
                    ENTERTAINMENT_TELEVISION           -> "Fernsehen"
                    ENTERTAINMENT_VIDEO_GAMES          -> "Videospiele"
                    ENTERTAINMENT_BOARD_GAMES          -> "Brettspiele"
                    ENTERTAINMENT_COMICS               -> "Comics"
                    ENTERTAINMENT_JAPANESE_ANIME_MANGA -> "Anime/Manga"
                    ENTERTAINMENT_CARTOON_ANIMATIONS   -> "Animationsfilme"
                    SCIENCE_AND_NATURE                 -> "Natur und Technik"
                    SCIENCE_COMPUTERS                  -> "Computer"
                    SCIENCE_MATHEMATICS                -> "Mathematik"
                    SCIENCE_GADGETS                    -> "Technische Geräte"
                    MYTHOLOGY                          -> "Mythologie"
                    SPORTS                             -> "Sport"
                    GEOGRAPHY                          -> "Geographie"
                    HISTORY                            -> "Geschichte"
                    POLITICS                           -> "Politik"
                    ART                                -> "Kunst"
                    CELEBRITIES                        -> "Berühmtheiten"
                    ANIMALS                            -> "Tiere"
                    VEHICLES                           -> "Fahrzeuge"
                }
            }
        }
    }
}