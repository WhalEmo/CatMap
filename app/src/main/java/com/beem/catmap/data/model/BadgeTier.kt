package com.beem.catmap.data.model

import androidx.annotation.ArrayRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.beem.catmap.R

enum class BadgeTier(
    val level: Int,
    val threshold: Long,

    @StringRes
    val titleResId: Int,

    @StringRes
    val elementResId: Int,

    @StringRes
    val shortDescriptionResId: Int,

    @StringRes
    val storyResId: Int,

    @ArrayRes
    val tagsResId: Int,

    @DrawableRes
    val iconResId: Int,

    @ColorRes
    val accentColorRes: Int,

    @ColorRes
    val pillBgColorRes: Int,

    @ColorRes
    val progressTrackBgRes: Int
) {

    TIER_01(
        level = 1,
        threshold = 1L,
        titleResId = R.string.badge_tier_01_title,
        elementResId = R.string.badge_tier_01_element,
        shortDescriptionResId = R.string.badge_tier_01_short_description,
        storyResId = R.string.badge_tier_01_story,
        tagsResId = R.array.badge_tier_01_tags,
        iconResId = R.drawable.catmap_badge_tier_01,
        accentColorRes = R.color.badge_tier_01_ui_accent,
        pillBgColorRes = R.color.badge_tier_01_ui_surface,
        progressTrackBgRes = R.color.badge_tier_01_ui_track
    ),

    TIER_02(
        level = 2,
        threshold = 5L,
        titleResId = R.string.badge_tier_02_title,
        elementResId = R.string.badge_tier_02_element,
        shortDescriptionResId = R.string.badge_tier_02_short_description,
        storyResId = R.string.badge_tier_02_story,
        tagsResId = R.array.badge_tier_02_tags,
        iconResId = R.drawable.catmap_badge_tier_02,
        accentColorRes = R.color.badge_tier_02_ui_accent,
        pillBgColorRes = R.color.badge_tier_02_ui_surface,
        progressTrackBgRes = R.color.badge_tier_02_ui_track
    ),

    TIER_03(
        level = 3,
        threshold = 10L,
        titleResId = R.string.badge_tier_03_title,
        elementResId = R.string.badge_tier_03_element,
        shortDescriptionResId = R.string.badge_tier_03_short_description,
        storyResId = R.string.badge_tier_03_story,
        tagsResId = R.array.badge_tier_03_tags,
        iconResId = R.drawable.catmap_badge_tier_03,
        accentColorRes = R.color.badge_tier_03_ui_accent,
        pillBgColorRes = R.color.badge_tier_03_ui_surface,
        progressTrackBgRes = R.color.badge_tier_03_ui_track
    ),

    TIER_04(
        level = 4,
        threshold = 15L,
        titleResId = R.string.badge_tier_04_title,
        elementResId = R.string.badge_tier_04_element,
        shortDescriptionResId = R.string.badge_tier_04_short_description,
        storyResId = R.string.badge_tier_04_story,
        tagsResId = R.array.badge_tier_04_tags,
        iconResId = R.drawable.catmap_badge_tier_04,
        accentColorRes = R.color.badge_tier_04_ui_accent,
        pillBgColorRes = R.color.badge_tier_04_ui_surface,
        progressTrackBgRes = R.color.badge_tier_04_ui_track
    ),

    TIER_05(
        level = 5,
        threshold = 20L,
        titleResId = R.string.badge_tier_05_title,
        elementResId = R.string.badge_tier_05_element,
        shortDescriptionResId = R.string.badge_tier_05_short_description,
        storyResId = R.string.badge_tier_05_story,
        tagsResId = R.array.badge_tier_05_tags,
        iconResId = R.drawable.catmap_badge_tier_05,
        accentColorRes = R.color.badge_tier_05_ui_accent,
        pillBgColorRes = R.color.badge_tier_05_ui_surface,
        progressTrackBgRes = R.color.badge_tier_05_ui_track
    ),

    TIER_06(
        level = 6,
        threshold = 25L,
        titleResId = R.string.badge_tier_06_title,
        elementResId = R.string.badge_tier_06_element,
        shortDescriptionResId = R.string.badge_tier_06_short_description,
        storyResId = R.string.badge_tier_06_story,
        tagsResId = R.array.badge_tier_06_tags,
        iconResId = R.drawable.catmap_badge_tier_06,
        accentColorRes = R.color.badge_tier_06_ui_accent,
        pillBgColorRes = R.color.badge_tier_06_ui_surface,
        progressTrackBgRes = R.color.badge_tier_06_ui_track
    ),

    TIER_07(
        level = 7,
        threshold = 30L,
        titleResId = R.string.badge_tier_07_title,
        elementResId = R.string.badge_tier_07_element,
        shortDescriptionResId = R.string.badge_tier_07_short_description,
        storyResId = R.string.badge_tier_07_story,
        tagsResId = R.array.badge_tier_07_tags,
        iconResId = R.drawable.catmap_badge_tier_07,
        accentColorRes = R.color.badge_tier_07_ui_accent,
        pillBgColorRes = R.color.badge_tier_07_ui_surface,
        progressTrackBgRes = R.color.badge_tier_07_ui_track
    ),

    TIER_08(
        level = 8,
        threshold = 40L,
        titleResId = R.string.badge_tier_08_title,
        elementResId = R.string.badge_tier_08_element,
        shortDescriptionResId = R.string.badge_tier_08_short_description,
        storyResId = R.string.badge_tier_08_story,
        tagsResId = R.array.badge_tier_08_tags,
        iconResId = R.drawable.catmap_badge_tier_08,
        accentColorRes = R.color.badge_tier_08_ui_accent,
        pillBgColorRes = R.color.badge_tier_08_ui_surface,
        progressTrackBgRes = R.color.badge_tier_08_ui_track
    );

    companion object {

        fun getTierForCatCount(catCount: Long): BadgeTier {
            return entries
                .lastOrNull { catCount >= it.threshold }
                ?: TIER_01
        }

        fun getNextTier(currentTier: BadgeTier): BadgeTier? {
            return entries.firstOrNull {
                it.level == currentTier.level + 1
            }
        }
    }
}