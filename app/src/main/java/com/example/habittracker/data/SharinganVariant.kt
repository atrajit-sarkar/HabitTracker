package it.atraj.habittracker.data

import it.atraj.habittracker.R

/**
 * Sharingan variants for Itachi theme
 */
enum class SharinganVariant(
    val displayName: String,
    val animationFile: String,
    val description: String,
    val previewImage: Int
) {
    ITACHI(
        displayName = "Itachi",
        animationFile = "animations/mangekyo_itachi.json",
        description = "Itachi's Mangekyo - Three tomoe pattern",
        previewImage = R.drawable.itachi_processed
    ),
    KAKASHI(
        displayName = "Kakashi", 
        animationFile = "animations/mangekyo_kakashi.json",
        description = "Kakashi's Mangekyo - Kamui pattern",
        previewImage = R.drawable.kakashi_processed
    ),
    OBITO(
        displayName = "Obito",
        animationFile = "animations/mangekyo_obito.json", 
        description = "Obito's Mangekyo - Kamui pattern",
        previewImage = R.drawable.obito_processed
    ),
    SASUKE(
        displayName = "Sasuke",
        animationFile = "animations/mangekyo_sasuke.json",
        description = "Sasuke's Mangekyo - Star pattern",
        previewImage = R.drawable.sasuke_processed
    ),
    MADARA(
        displayName = "Madara",
        animationFile = "animations/mangekyo_madara.json",
        description = "Madara's Mangekyo - Eternal pattern",
        previewImage = R.drawable.madara_processed
    );
    
    companion object {
        fun fromString(name: String): SharinganVariant {
            return values().find { it.name == name } ?: KAKASHI
        }
    }
}
