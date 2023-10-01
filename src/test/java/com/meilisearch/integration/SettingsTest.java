package com.meilisearch.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.meilisearch.integration.classes.AbstractIT;
import com.meilisearch.integration.classes.TestData;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.Faceting;
import com.meilisearch.sdk.model.Pagination;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TypoTolerance;
import com.meilisearch.sdk.utils.Movie;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public class SettingsTest extends AbstractIT {

    private TestData<Movie> testData;

    @BeforeEach
    public void initialize() {
        this.setUp();
        if (testData == null) testData = this.getTestData(MOVIES_INDEX, Movie.class);
    }

    @AfterAll
    static void cleanMeilisearch() {
        cleanup();
    }

    /** Tests of the setting methods */
    @Test
    @DisplayName("Test get settings from an index by uid")
    public void testGetSettings() throws Exception {
        Index index = createIndex("getSettings");
        Settings settings = index.getSettings();

        assertThat(settings.getRankingRules(), is(arrayWithSize(6)));
    }

    @Test
    @DisplayName("Test update settings changing the ranking rules")
    public void testUpdateSettingsRankingRules() throws Exception {
        Index index = createIndex("updateSettingsRankingRules");
        Settings settings = index.getSettings();
        settings.setRankingRules(
                "typo",
                "words",
                "sort",
                "proximity",
                "attribute",
                "exactness",
                "release_date:desc",
                "rank:desc");
        index.waitForTask(index.updateSettings(settings).getTaskUid());
        Settings newSettings = index.getSettings();
        assertThat(newSettings.getRankingRules(), is(arrayWithSize(8)));
    }

    @Test
    @DisplayName("Test update settings changing the synonyms")
    public void testUpdateSettingsSynonyms() throws Exception {
        Index index = createIndex("updateSettingsSynonyms");
        Settings settings = index.getSettings();

        HashMap<String, String[]> synonyms = new HashMap<>();
        synonyms.put("wolverine", new String[] {"xmen", "logan"});
        synonyms.put("logan", new String[] {"wolverine"});
        settings.setSynonyms(synonyms);

        index.waitForTask(index.updateSettings(settings).getTaskUid());

        Settings newSettings = index.getSettings();

        assertThat(newSettings.getSynonyms(), is(aMapWithSize(2)));
    }

    @Test
    @DisplayName("Test update settings changing the sort")
    public void testUpdateSettingsSort() throws Exception {
        Index index = createIndex("updateSettingsSort");
        Settings settings = index.getSettings();
        settings.setSortableAttributes("title", "year");

        index.waitForTask(index.updateSettings(settings).getTaskUid());

        Settings newSettings = index.getSettings();

        assertThat(newSettings.getSortableAttributes(), is(arrayWithSize(2)));
    }

    @Test
    @DisplayName("Test update settings changing the typo tolerance")
    public void testUpdateSettingsTypoTolerance() throws Exception {
        Index index = createIndex("updateSettingsTypoTolerance");
        Settings settings = index.getSettings();

        TypoTolerance typoTolerance = new TypoTolerance();
        typoTolerance.setDisableOnWords("and");
        typoTolerance.setDisableOnAttributes("title");
        settings.setTypoTolerance(typoTolerance);

        index.waitForTask(index.updateSettings(settings).getTaskUid());

        Settings newSettings = index.getSettings();

        assertThat(newSettings.getTypoTolerance().getDisableOnWords(), is(arrayWithSize(1)));
        assertThat(newSettings.getTypoTolerance().getDisableOnAttributes(), is(arrayWithSize(1)));
        assertThat(newSettings.getTypoTolerance().isEnabled(), is(equalTo(true)));
    }

    @Test
    @DisplayName("Test update multiple settings in a row")
    public void testUpdateMultipleSettingsInARow() throws Exception {
        Index index = createIndex("updateMultipleSettingsInARow");
        Settings settingsDisplayedAttr = new Settings();
        settingsDisplayedAttr.setDisplayedAttributes("title", "overview", "genres", "release_date");
        index.waitForTask(index.updateSettings(settingsDisplayedAttr).getTaskUid());
        Settings newSettingsDisplayedAttr = index.getSettings();

        Settings settingsRankingRules = new Settings();
        settingsRankingRules.setRankingRules(
                "typo",
                "words",
                "sort",
                "proximity",
                "attribute",
                "exactness",
                "release_date:desc",
                "rank:desc");
        index.waitForTask(index.updateSettings(settingsRankingRules).getTaskUid());
        Settings newSettingsRankingRules = index.getSettings();

        Settings settingsSynonyms = new Settings();
        HashMap<String, String[]> synonyms = new HashMap<>();
        synonyms.put("wolverine", new String[] {"xmen", "logan"});
        synonyms.put("logan", new String[] {"wolverine"});
        settingsSynonyms.setSynonyms(synonyms);
        index.waitForTask(index.updateSettings(settingsSynonyms).getTaskUid());
        Settings newSettingsSynonyms = index.getSettings();

        assertThat(newSettingsDisplayedAttr.getDisplayedAttributes(), is(arrayWithSize(4)));
        assertThat(newSettingsDisplayedAttr.getRankingRules(), is(arrayWithSize(6)));
        assertThat(newSettingsDisplayedAttr.getSynonyms(), is(anEmptyMap()));
        assertThat(newSettingsRankingRules.getDisplayedAttributes(), is(arrayWithSize(4)));
        assertThat(newSettingsRankingRules.getRankingRules(), is(arrayWithSize(8)));
        assertThat(newSettingsRankingRules.getSynonyms(), is(anEmptyMap()));
        assertThat(newSettingsSynonyms.getDisplayedAttributes(), is(arrayWithSize(4)));
        assertThat(newSettingsSynonyms.getSynonyms(), is(aMapWithSize(2)));
        assertThat(newSettingsSynonyms.getRankingRules(), is(arrayWithSize(8)));
    }

    @Test
    @DisplayName("Test reset settings")
    public void testResetSettings() throws Exception {
        Index index = createIndex("testResetSettings");
        Settings initialSettings = index.getSettings();

        Settings settingsWithSynonyms = new Settings();
        HashMap<String, String[]> synonyms = new HashMap<>();
        synonyms.put("wolverine", new String[] {"xmen", "logan"});
        synonyms.put("logan", new String[] {"wolverine"});
        settingsWithSynonyms.setSynonyms(synonyms);

        index.waitForTask(index.updateSettings(settingsWithSynonyms).getTaskUid());
        settingsWithSynonyms = index.getSettings();
        assertThat(settingsWithSynonyms.getSynonyms(), is(aMapWithSize(2)));

        index.waitForTask(index.resetSettings().getTaskUid());
        Settings settingsAfterReset = index.getSettings();
        assertThat(
                settingsAfterReset.getSynonyms().size(),
                equalTo(initialSettings.getSynonyms().size()));
    }

    /** Tests of the ranking rules setting methods */
    @Test
    @DisplayName("Test get ranking rules settings by uid")
    public void testGetRankingRulesSettings() throws Exception {
        Index index = createIndex("testGetRankingRulesSettings");
        Settings initialSettings = index.getSettings();
        String[] initialRankingRules = index.getRankingRulesSettings();

        assertThat(
                initialRankingRules, is(arrayWithSize(initialSettings.getRankingRules().length)));
        assertThat(initialRankingRules, is(equalTo(initialSettings.getRankingRules())));
    }

    @Test
    @DisplayName("Test update ranking rules settings")
    public void testUpdateRankingRulesSettings() throws Exception {
        Index index = createIndex("testUpdateRankingRulesSettings");
        String[] initialRulesSettings = index.getRankingRulesSettings();
        String[] newRankingRules = {
            "typo",
            "words",
            "sort",
            "proximity",
            "attribute",
            "exactness",
            "release_date:desc",
            "rank:desc"
        };

        index.waitForTask(index.updateRankingRulesSettings(newRankingRules).getTaskUid());
        String[] updatedRankingRulesSettings = index.getRankingRulesSettings();

        assertThat(updatedRankingRulesSettings, is(arrayWithSize(newRankingRules.length)));
        assertThat(updatedRankingRulesSettings, is(equalTo(newRankingRules)));
        assertThat(
                updatedRankingRulesSettings, is(not(arrayWithSize(initialRulesSettings.length))));
    }

    @Test
    @DisplayName("Test reset ranking rules settings")
    public void testResetRankingRulesSettings() throws Exception {
        Index index = createIndex("testResetRankingRulesSettings");
        String[] initialRulesSettings = index.getRankingRulesSettings();
        String[] newRankingRules = {
            "typo",
            "words",
            "sort",
            "proximity",
            "attribute",
            "exactness",
            "release_date:desc",
            "rank:desc"
        };

        index.waitForTask(index.updateRankingRulesSettings(newRankingRules).getTaskUid());
        String[] updatedRankingRulesSettings = index.getRankingRulesSettings();

        index.waitForTask(index.resetRankingRulesSettings().getTaskUid());
        String[] rankingRulesAfterReset = index.getRankingRulesSettings();

        assertThat(updatedRankingRulesSettings, is(arrayWithSize(newRankingRules.length)));
        assertThat(updatedRankingRulesSettings, is(equalTo(newRankingRules)));
        assertThat(
                updatedRankingRulesSettings, is(not(arrayWithSize(initialRulesSettings.length))));
        assertThat(
                rankingRulesAfterReset, is(not(arrayWithSize(updatedRankingRulesSettings.length))));
        assertThat(rankingRulesAfterReset, is(arrayWithSize(initialRulesSettings.length)));
        assertThat(rankingRulesAfterReset, is(equalTo(initialRulesSettings)));
    }

    /** Tests of the synonyms setting methods */
    @Test
    @DisplayName("Test get synonyms settings by uid")
    public void testGetSynonymsSettings() throws Exception {
        Index index = createIndex("testGetSynonymsSettings");
        Settings initialSettings = index.getSettings();
        Map<String, String[]> synonymsSettings = index.getSynonymsSettings();

        assertThat(synonymsSettings, is(aMapWithSize(initialSettings.getSynonyms().size())));
        assertThat(synonymsSettings, is(equalTo(initialSettings.getSynonyms())));
    }

    @Test
    @DisplayName("Test update synonyms settings")
    public void testUpdateSynonymsSettings() throws Exception {
        Index index = createIndex("testUpdateSynonymsSettings");
        Map<String, String[]> synonymsSettings = index.getSynonymsSettings();
        HashMap<String, String[]> newSynonymsSettings = new HashMap<>();
        newSynonymsSettings.put("wolverine", new String[] {"xmen", "logan"});
        newSynonymsSettings.put("logan", new String[] {"wolverine", "xmen"});
        newSynonymsSettings.put("wow", new String[] {"world of warcraft"});

        index.waitForTask(index.updateSynonymsSettings(newSynonymsSettings).getTaskUid());
        Map<String, String[]> updatedSynonymsSettings = index.getSynonymsSettings();

        assertThat(updatedSynonymsSettings, is(aMapWithSize(newSynonymsSettings.size())));
        assertThat(updatedSynonymsSettings.keySet(), is(equalTo(newSynonymsSettings.keySet())));
        assertThat(updatedSynonymsSettings, is(not(aMapWithSize(synonymsSettings.size()))));
        assertThat(updatedSynonymsSettings.keySet(), is(not(equalTo(synonymsSettings.keySet()))));
    }

    @Test
    @DisplayName("Test reset synonyms settings")
    public void testResetSynonymsSettings() throws Exception {
        Index index = createIndex("testResetSynonymsSettings");
        Map<String, String[]> synonymsSettings = index.getSynonymsSettings();
        HashMap<String, String[]> newSynonymsSettings = new HashMap<>();
        newSynonymsSettings.put("wolverine", new String[] {"xmen", "logan"});
        newSynonymsSettings.put("logan", new String[] {"wolverine", "xmen"});
        newSynonymsSettings.put("wow", new String[] {"world of warcraft"});

        index.waitForTask(index.updateSynonymsSettings(newSynonymsSettings).getTaskUid());
        Map<String, String[]> updatedSynonymsSettings = index.getSynonymsSettings();

        index.waitForTask(index.resetSynonymsSettings().getTaskUid());
        Map<String, String[]> synonymsSettingsAfterReset = index.getSynonymsSettings();

        assertThat(updatedSynonymsSettings, is(aMapWithSize(newSynonymsSettings.size())));
        assertThat(updatedSynonymsSettings.keySet(), is(equalTo(newSynonymsSettings.keySet())));
        assertThat(updatedSynonymsSettings, is(not(aMapWithSize(synonymsSettings.size()))));
        assertThat(updatedSynonymsSettings.keySet(), is(not(equalTo(synonymsSettings.keySet()))));
        assertThat(
                synonymsSettingsAfterReset, is(not(aMapWithSize(updatedSynonymsSettings.size()))));
        assertThat(synonymsSettingsAfterReset, is(aMapWithSize(synonymsSettings.size())));
        assertThat(synonymsSettingsAfterReset.keySet(), is(equalTo(synonymsSettings.keySet())));
    }

    /** Tests of the stop words setting methods */
    @Test
    @DisplayName("Test get stop-words settings by uid")
    public void testGetStopWordsSettings() throws Exception {
        Index index = createIndex("testGetStopWordsSettings");
        Settings initialSettings = index.getSettings();
        String[] initialStopWords = index.getStopWordsSettings();

        assertThat(initialStopWords, is(arrayWithSize(initialSettings.getStopWords().length)));
        assertThat(initialStopWords, is(equalTo(initialSettings.getStopWords())));
    }

    @Test
    @DisplayName("Test update stop-words settings")
    public void testUpdateStopWordsSettings() throws Exception {
        Index index = createIndex("testUpdateStopWordsSettings");
        String[] initialStopWords = index.getStopWordsSettings();
        String[] newStopWords = {"of", "the", "to"};

        index.waitForTask(index.updateStopWordsSettings(newStopWords).getTaskUid());
        String[] updatedStopWordsSettings = index.getStopWordsSettings();

        assertThat(updatedStopWordsSettings, is(arrayWithSize(newStopWords.length)));
        assertThat(updatedStopWordsSettings, is(equalTo(newStopWords)));
        assertThat(updatedStopWordsSettings, is(not(arrayWithSize(initialStopWords.length))));
    }

    @Test
    @DisplayName("Test reset stop-words settings")
    public void testResetStopWordsSettings() throws Exception {
        Index index = createIndex("testResetStopWordsSettings");
        String[] initialStopWords = index.getStopWordsSettings();
        String[] newStopWords = {"of", "the", "to"};

        index.waitForTask(index.updateStopWordsSettings(newStopWords).getTaskUid());
        String[] updatedStopWordsSettings = index.getStopWordsSettings();

        index.waitForTask(index.resetStopWordsSettings().getTaskUid());
        String[] stopWordsAfterReset = index.getStopWordsSettings();

        assertThat(updatedStopWordsSettings, is(arrayWithSize(newStopWords.length)));
        assertThat(updatedStopWordsSettings, is(equalTo(newStopWords)));
        assertThat(updatedStopWordsSettings, is(not(arrayWithSize(initialStopWords.length))));
        assertThat(stopWordsAfterReset, is(not(arrayWithSize(updatedStopWordsSettings.length))));
        assertThat(stopWordsAfterReset, is(arrayWithSize(initialStopWords.length)));
        assertThat(stopWordsAfterReset, is(equalTo(initialStopWords)));
    }

    /** Tests of the searchable attributes setting methods */
    @Test
    @DisplayName("Test get searchable attributes settings by uid")
    public void testGetSearchableAttributesSettings() throws Exception {
        Index index = createIndex("testGetSearchableAttributesSettings");
        Settings initialSettings = index.getSettings();
        String[] initialSearchableAttributes = index.getSearchableAttributesSettings();

        assertThat(
                initialSearchableAttributes,
                is(arrayWithSize(initialSettings.getSearchableAttributes().length)));
        assertThat(
                initialSearchableAttributes,
                is(equalTo(initialSettings.getSearchableAttributes())));
    }

    @Test
    @DisplayName("Test update searchable attributes settings")
    public void testUpdateSearchableAttributesSettings() throws Exception {
        Index index = createIndex("testUpdateSearchableAttributesSettings");
        String[] initialSearchableAttributes = index.getSearchableAttributesSettings();
        String[] newSearchableAttributes = {"title", "description", "genre"};

        index.waitForTask(
                index.updateSearchableAttributesSettings(newSearchableAttributes).getTaskUid());
        String[] updatedSearchableAttributes = index.getSearchableAttributesSettings();

        assertThat(updatedSearchableAttributes, is(arrayWithSize(newSearchableAttributes.length)));
        assertThat(updatedSearchableAttributes, is(equalTo(newSearchableAttributes)));
        assertThat(
                updatedSearchableAttributes,
                is(not(arrayWithSize(initialSearchableAttributes.length))));
    }

    @Test
    @DisplayName("Test reset searchable attributes settings")
    public void testResetSearchableAttributesSettings() throws Exception {
        Index index = createIndex("testUpdateSearchableAttributesSettings");
        String[] initialSearchableAttributes = index.getSearchableAttributesSettings();
        String[] newSearchableAttributes = {"title", "description", "genre"};

        index.waitForTask(
                index.updateSearchableAttributesSettings(newSearchableAttributes).getTaskUid());
        String[] updatedSearchableAttributes = index.getSearchableAttributesSettings();

        index.waitForTask(index.resetSearchableAttributesSettings().getTaskUid());
        String[] searchableAttributesAfterReset = index.getSearchableAttributesSettings();

        assertThat(updatedSearchableAttributes, is(arrayWithSize(newSearchableAttributes.length)));
        assertThat(updatedSearchableAttributes, is(equalTo(newSearchableAttributes)));
        assertThat(
                updatedSearchableAttributes,
                is(not(arrayWithSize(initialSearchableAttributes.length))));
        assertThat(
                searchableAttributesAfterReset,
                is(not(arrayWithSize(updatedSearchableAttributes.length))));
        assertThat(
                searchableAttributesAfterReset,
                is(arrayWithSize(initialSearchableAttributes.length)));
        assertThat(searchableAttributesAfterReset, is(equalTo(initialSearchableAttributes)));
    }

    /** Tests of the display attributes setting methods */
    @Test
    @DisplayName("Test get display attributes settings by uid")
    public void testGetDisplayedAttributesSettings() throws Exception {
        Index index = createIndex("testGetDisplayedAttributesSettings");
        Settings initialSettings = index.getSettings();
        String[] initialDisplayedAttributes = index.getDisplayedAttributesSettings();

        assertThat(
                initialDisplayedAttributes,
                is(arrayWithSize(initialSettings.getSearchableAttributes().length)));
        assertThat(
                initialDisplayedAttributes, is(equalTo(initialSettings.getDisplayedAttributes())));
    }

    @Test
    @DisplayName("Test update display attributes settings")
    public void testUpdateDisplayedAttributesSettings() throws Exception {
        Index index = createIndex("testUpdateDisplayedAttributesSettings");
        String[] initialDisplayedAttributes = index.getDisplayedAttributesSettings();
        String[] newDisplayedAttributes = {"title", "description", "genre", "release_date"};

        index.waitForTask(
                index.updateDisplayedAttributesSettings(newDisplayedAttributes).getTaskUid());
        String[] updatedDisplayedAttributes = index.getDisplayedAttributesSettings();

        assertThat(updatedDisplayedAttributes, is(arrayWithSize(newDisplayedAttributes.length)));
        assertThat(updatedDisplayedAttributes, is(equalTo(newDisplayedAttributes)));
        assertThat(
                updatedDisplayedAttributes,
                is(not(arrayWithSize(initialDisplayedAttributes.length))));
    }

    @Test
    @DisplayName("Test reset display attributes settings")
    public void testResetDisplayedAttributesSettings() throws Exception {
        Index index = createIndex("testUpdateDisplayedAttributesSettings");
        String[] initialDisplayedAttributes = index.getDisplayedAttributesSettings();
        String[] newDisplayedAttributes = {"title", "description", "genre", "release_date", "cast"};

        index.waitForTask(
                index.updateDisplayedAttributesSettings(newDisplayedAttributes).getTaskUid());
        String[] updatedDisplayedAttributes = index.getDisplayedAttributesSettings();

        index.waitForTask(index.resetDisplayedAttributesSettings().getTaskUid());
        String[] displayedAttributesAfterReset = index.getDisplayedAttributesSettings();

        assertThat(updatedDisplayedAttributes, is(arrayWithSize(newDisplayedAttributes.length)));
        assertThat(updatedDisplayedAttributes, is(equalTo(newDisplayedAttributes)));
        assertThat(
                updatedDisplayedAttributes,
                is(not(arrayWithSize(initialDisplayedAttributes.length))));
        assertThat(
                displayedAttributesAfterReset,
                is(not(arrayWithSize(updatedDisplayedAttributes.length))));
    }

    /** Tests of the filterable attributes setting methods */
    @Test
    @DisplayName("Test get filterable attributes settings by uid")
    public void testGetFilterableAttributesSettings() throws Exception {
        Index index = createIndex("testGetDisplayedAttributesSettings");
        Settings initialSettings = index.getSettings();
        String[] initialFilterableAttributes = index.getFilterableAttributesSettings();

        assertThat(
                initialFilterableAttributes,
                is(arrayWithSize(initialSettings.getFilterableAttributes().length)));
        assertThat(
                initialFilterableAttributes,
                is(equalTo(initialSettings.getFilterableAttributes())));
    }

    @Test
    @DisplayName("Test update filterable attributes settings")
    public void testUpdateFilterableAttributesSettings() throws Exception {
        Index index = createIndex("testUpdateDisplayedAttributesSettings");
        String[] initialFilterableAttributes = index.getFilterableAttributesSettings();
        String[] newFilterableAttributes = {"title", "description", "genre", "release_date"};

        index.waitForTask(
                index.updateFilterableAttributesSettings(newFilterableAttributes).getTaskUid());
        String[] updatedFilterableAttributes = index.getFilterableAttributesSettings();

        assertThat(updatedFilterableAttributes, is(arrayWithSize(newFilterableAttributes.length)));
        assertThat(
                Arrays.asList(newFilterableAttributes),
                containsInAnyOrder(updatedFilterableAttributes));
        assertThat(
                updatedFilterableAttributes,
                is(not(arrayWithSize(initialFilterableAttributes.length))));
    }

    @Test
    @DisplayName("Test reset filterable attributes settings")
    public void testResetFilterableAttributesSettings() throws Exception {
        Index index = createIndex("testUpdateDisplayedAttributesSettings");
        String[] initialFilterableAttributes = index.getFilterableAttributesSettings();
        String[] newFilterableAttributes = {
            "title", "description", "genres", "director", "release_date"
        };

        index.waitForTask(
                index.updateFilterableAttributesSettings(newFilterableAttributes).getTaskUid());
        String[] updatedFilterableAttributes = index.getFilterableAttributesSettings();

        index.waitForTask(index.resetFilterableAttributesSettings().getTaskUid());
        String[] filterableAttributesAfterReset = index.getFilterableAttributesSettings();

        assertThat(updatedFilterableAttributes, is(arrayWithSize(newFilterableAttributes.length)));
        assertThat(
                Arrays.asList(newFilterableAttributes),
                containsInAnyOrder(updatedFilterableAttributes));
        assertThat(
                updatedFilterableAttributes,
                is(not(arrayWithSize(initialFilterableAttributes.length))));
        assertThat(
                filterableAttributesAfterReset,
                is(not(arrayWithSize(updatedFilterableAttributes.length))));
        assertThat(
                updatedFilterableAttributes,
                is(not(arrayWithSize(initialFilterableAttributes.length))));
    }

    /** Tests of the sortable attributes setting methods* */
    @Test
    @DisplayName("Test get sortable attributes settings by uid")
    public void testGetSortableAttributesSettings() throws Exception {
        Index index = createIndex("testGetSortableAttributesSettings");
        Settings initialSettings = index.getSettings();
        String[] initialSortableAttributes = index.getSortableAttributesSettings();

        assertThat(
                initialSortableAttributes,
                is(arrayWithSize(initialSettings.getSortableAttributes().length)));
        assertThat(initialSortableAttributes, is(equalTo(initialSettings.getSortableAttributes())));
    }

    @Test
    @DisplayName("Test update sortable attributes settings")
    public void testUpdateSortableAttributesSettings() throws Exception {
        Index index = createIndex("testUpdateSortableAttributesSettings");
        String[] initialSortableAttributes = index.getSortableAttributesSettings();
        String[] newSortableAttributes = {"title", "description", "genre", "release_date"};

        index.waitForTask(
                index.updateSortableAttributesSettings(newSortableAttributes).getTaskUid());
        String[] updatedSortableAttributes = index.getSortableAttributesSettings();

        assertThat(updatedSortableAttributes, is(arrayWithSize(newSortableAttributes.length)));
        assertThat(
                Arrays.asList(newSortableAttributes),
                containsInAnyOrder(updatedSortableAttributes));
        assertThat(
                updatedSortableAttributes,
                is(not(arrayWithSize(initialSortableAttributes.length))));
    }

    @Test
    @DisplayName("Test reset sortable attributes settings")
    public void testResetSortableAttributesSettings() throws Exception {
        Index index = createIndex("testUpdateSortableAttributesSettings");
        String[] initialSortableAttributes = index.getSortableAttributesSettings();
        String[] newSortableAttributes = {
            "title", "description", "genres", "director", "release_date"
        };

        index.waitForTask(
                index.updateSortableAttributesSettings(newSortableAttributes).getTaskUid());
        String[] updatedSortableAttributes = index.getSortableAttributesSettings();

        index.waitForTask(index.resetFilterableAttributesSettings().getTaskUid());
        String[] filterableAttributesAfterReset = index.getFilterableAttributesSettings();

        assertThat(updatedSortableAttributes, is(arrayWithSize(newSortableAttributes.length)));
        assertThat(
                Arrays.asList(newSortableAttributes),
                containsInAnyOrder(updatedSortableAttributes));
        assertThat(
                updatedSortableAttributes,
                is(not(arrayWithSize(initialSortableAttributes.length))));
        assertThat(
                filterableAttributesAfterReset,
                is(not(arrayWithSize(updatedSortableAttributes.length))));
        assertThat(
                updatedSortableAttributes,
                is(not(arrayWithSize(initialSortableAttributes.length))));
    }

    /** Tests of the distinct attributes setting methods */
    @Test
    @DisplayName("Test get distinct attribute settings by uid")
    public void testGetDistinctAttributeSettings() throws Exception {
        Index index = createIndex("testGetDistinctAttributeSettings");
        Settings initialSettings = index.getSettings();
        String initialDistinctAttribute = index.getDistinctAttributeSettings();

        assertThat(initialDistinctAttribute, is(equalTo(initialSettings.getDistinctAttribute())));
    }

    @Test
    @DisplayName("Test update distinct attribute settings")
    public void testUpdateDistinctAttributeSettings() throws Exception {
        Index index = createIndex("testUpdateDistinctAttributeSettings");
        String initialDistinctAttribute = index.getDistinctAttributeSettings();
        String newDistinctAttribute = "title";

        index.waitForTask(index.updateDistinctAttributeSettings(newDistinctAttribute).getTaskUid());
        String updatedDistinctAttribute = index.getDistinctAttributeSettings();

        assertThat(updatedDistinctAttribute, is(equalTo(newDistinctAttribute)));
        assertThat(updatedDistinctAttribute, is(not(equalTo(initialDistinctAttribute))));
    }

    @Test
    @DisplayName("Test reset distinct attribute settings")
    public void testResetDistinctAttributeSettings() throws Exception {
        Index index = createIndex("testResetDistinctAttributeSettings");
        String initialDistinctAttribute = index.getDistinctAttributeSettings();
        String newDistinctAttribute = "title";

        index.waitForTask(index.updateDistinctAttributeSettings(newDistinctAttribute).getTaskUid());
        String updatedDistinctAttribute = index.getDistinctAttributeSettings();

        index.waitForTask(index.resetDistinctAttributeSettings().getTaskUid());
        String distinctAttributeAfterReset = index.getDistinctAttributeSettings();

        assertThat(updatedDistinctAttribute, is(equalTo(newDistinctAttribute)));
        assertThat(updatedDistinctAttribute, is(not(equalTo(initialDistinctAttribute))));
        assertThat(distinctAttributeAfterReset, is(not(equalTo(updatedDistinctAttribute))));
        assertThat(updatedDistinctAttribute, is(not(equalTo(initialDistinctAttribute))));
    }

    /** Tests of the typo tolerance setting methods */
    @Test
    @DisplayName("Test get typo tolerance settings by uid")
    public void testGetTypoTolerance() throws Exception {
        Index index = createIndex("testGetTypoTolerance");
        Settings initialSettings = index.getSettings();
        TypoTolerance initialTypoTolerance = index.getTypoToleranceSettings();

        assertThat(initialSettings.getTypoTolerance().getDisableOnWords(), is(emptyArray()));
        assertThat(initialTypoTolerance.getDisableOnWords(), is(emptyArray()));
        assertThat(initialSettings.getTypoTolerance().getDisableOnAttributes(), is(emptyArray()));
        assertThat(initialTypoTolerance.getDisableOnAttributes(), is(emptyArray()));
        assertThat(
                initialTypoTolerance.isEnabled(),
                is(equalTo(initialSettings.getTypoTolerance().isEnabled())));
        assertThat(
                initialTypoTolerance.getMinWordSizeForTypos().containsKey("oneTypo"),
                is(notNullValue()));
        assertThat(
                initialTypoTolerance.getMinWordSizeForTypos().get("oneTypo"), is(notNullValue()));
        assertThat(
                initialTypoTolerance.getMinWordSizeForTypos().containsKey("twoTypos"),
                is(notNullValue()));
        assertThat(
                initialTypoTolerance.getMinWordSizeForTypos().get("twoTypos"), is(notNullValue()));
    }

    @Test
    @DisplayName("Test update typo tolerance settings")
    public void testUpdateTypoTolerance() throws Exception {
        Index index = createIndex("testUpdateTypoTolerance");
        TypoTolerance newTypoTolerance = new TypoTolerance();
        newTypoTolerance.setEnabled(true);
        newTypoTolerance.setDisableOnWords("and");
        newTypoTolerance.setDisableOnAttributes("title");

        HashMap<String, Integer> minWordSizeTypos =
                new HashMap<String, Integer>() {
                    {
                        put("oneTypo", 7);
                        put("twoTypos", 10);
                    }
                };
        newTypoTolerance.setMinWordSizeForTypos(minWordSizeTypos);
        index.waitForTask(index.updateTypoToleranceSettings(newTypoTolerance).getTaskUid());
        TypoTolerance updatedTypoTolerance = index.getTypoToleranceSettings();

        assertThat(
                updatedTypoTolerance.getDisableOnWords()[0],
                is(equalTo(newTypoTolerance.getDisableOnWords()[0])));
        assertThat(
                updatedTypoTolerance.getDisableOnAttributes()[0],
                is(equalTo(newTypoTolerance.getDisableOnAttributes()[0])));
        assertThat(updatedTypoTolerance.isEnabled(), is(equalTo(true)));
        assertThat(updatedTypoTolerance.getMinWordSizeForTypos(), hasKey("oneTypo"));
        assertThat(updatedTypoTolerance.getMinWordSizeForTypos().get("oneTypo"), is(equalTo(7)));
        assertThat(updatedTypoTolerance.getMinWordSizeForTypos(), hasKey("twoTypos"));
        assertThat(updatedTypoTolerance.getMinWordSizeForTypos().get("twoTypos"), is(equalTo(10)));
    }

    @Test
    @DisplayName("Test update typo tolerance settings")
    public void testPartialUpdateTypoTolerance() throws Exception {
        Index index = createIndex("testUpdateTypoTolerance");
        TypoTolerance newTypoTolerance = new TypoTolerance();
        newTypoTolerance.setDisableOnWords("the");
        newTypoTolerance.setDisableOnAttributes("title");

        index.waitForTask(index.updateTypoToleranceSettings(newTypoTolerance).getTaskUid());
        TypoTolerance updatedTypoTolerance = index.getTypoToleranceSettings();

        assertThat(
                updatedTypoTolerance.getDisableOnWords()[0],
                is(equalTo(newTypoTolerance.getDisableOnWords()[0])));
        assertThat(
                updatedTypoTolerance.getDisableOnAttributes()[0],
                is(equalTo(newTypoTolerance.getDisableOnAttributes()[0])));
        assertThat(updatedTypoTolerance.isEnabled(), is(equalTo(true)));
        assertThat(updatedTypoTolerance.getMinWordSizeForTypos(), hasKey("oneTypo"));
        assertThat(updatedTypoTolerance.getMinWordSizeForTypos().get("oneTypo"), is(equalTo(5)));
        assertThat(updatedTypoTolerance.getMinWordSizeForTypos(), hasKey("twoTypos"));
        assertThat(updatedTypoTolerance.getMinWordSizeForTypos().get("twoTypos"), is(equalTo(9)));
    }

    @Test
    @DisplayName("Test reset typo tolerance settings")
    public void testResetTypoTolerance() throws Exception {
        Index index = createIndex("testResetTypoTolerance");

        TypoTolerance initialTypoTolerance = index.getTypoToleranceSettings();
        TypoTolerance newTypoTolerance = new TypoTolerance();
        newTypoTolerance.setEnabled(true);
        newTypoTolerance.setDisableOnWords("and");
        newTypoTolerance.setDisableOnAttributes("title");
        HashMap<String, Integer> minWordSizeTypos =
                new HashMap<String, Integer>() {
                    {
                        put("oneTypo", 7);
                        put("twoTypos", 10);
                    }
                };
        newTypoTolerance.setMinWordSizeForTypos(minWordSizeTypos);

        index.waitForTask(index.updateTypoToleranceSettings(newTypoTolerance).getTaskUid());
        TypoTolerance updatedTypoTolerance = index.getTypoToleranceSettings();

        index.waitForTask(index.resetTypoToleranceSettings().getTaskUid());
        TypoTolerance typoToleranceAfterReset = index.getTypoToleranceSettings();

        assertThat(
                updatedTypoTolerance.getDisableOnWords(),
                is(arrayWithSize(newTypoTolerance.getDisableOnWords().length)));
        assertThat(
                updatedTypoTolerance.getDisableOnAttributes(),
                is(arrayWithSize(newTypoTolerance.getDisableOnAttributes().length)));
        assertThat(updatedTypoTolerance.isEnabled(), is(equalTo(newTypoTolerance.isEnabled())));

        assertThat(
                typoToleranceAfterReset.getDisableOnWords(),
                is(arrayWithSize(initialTypoTolerance.getDisableOnWords().length)));
        assertThat(
                typoToleranceAfterReset.getDisableOnAttributes(),
                is(arrayWithSize(initialTypoTolerance.getDisableOnAttributes().length)));
        assertThat(
                typoToleranceAfterReset.isEnabled(), is(equalTo(initialTypoTolerance.isEnabled())));
        assertThat(typoToleranceAfterReset.getMinWordSizeForTypos(), hasKey("oneTypo"));
        assertThat(
                typoToleranceAfterReset.getMinWordSizeForTypos().get("oneTypo"),
                is(notNullValue()));
        assertThat(typoToleranceAfterReset.getMinWordSizeForTypos(), hasKey("twoTypos"));
        assertThat(
                typoToleranceAfterReset.getMinWordSizeForTypos().get("twoTypos"),
                is(notNullValue()));
    }

    /** Tests of all the specifics setting methods when null is passed */
    @Test
    @DisplayName("Test update synonyms settings when null is passed")
    public void testUpdateSynonymsSettingsUsingNull() throws Exception {
        Index index = createIndex("testUpdateSynonymsSettingsUsingNull");
        Map<String, String[]> initialSynonymsSettings = index.getSynonymsSettings();
        HashMap<String, String[]> newSynonymsSettings = new HashMap<>();
        newSynonymsSettings.put("007", new String[] {"james bond", "bond"});
        newSynonymsSettings.put("ironman", new String[] {"tony stark", "iron man"});

        index.waitForTask(index.updateSynonymsSettings(newSynonymsSettings).getTaskUid());
        Map<String, String[]> updatedSynonymsSettings = index.getSynonymsSettings();

        index.waitForTask(index.updateSynonymsSettings(null).getTaskUid());
        Map<String, String[]> resetSynonymsSettings = index.getSynonymsSettings();

        assertThat(updatedSynonymsSettings, is(not(aMapWithSize(initialSynonymsSettings.size()))));
        assertThat(resetSynonymsSettings, is(not(aMapWithSize(updatedSynonymsSettings.size()))));
        assertThat(resetSynonymsSettings, is(aMapWithSize(initialSynonymsSettings.size())));
        assertThat(resetSynonymsSettings.keySet(), is(equalTo(initialSynonymsSettings.keySet())));
    }

    @Test
    @DisplayName("Test update stop-words settings when null is passed")
    public void testUpdateStopWordsSettingsUsingNull() throws Exception {
        Index index = createIndex("testUpdateStopWordsSettingsUsingNull");
        String[] initialStopWords = index.getStopWordsSettings();
        String[] newStopWords = {"the", "to", "in", "on"};

        index.waitForTask(index.updateStopWordsSettings(newStopWords).getTaskUid());
        String[] updatedStopWords = index.getStopWordsSettings();

        index.waitForTask(index.updateStopWordsSettings(null).getTaskUid());
        String[] resetStopWords = index.getStopWordsSettings();

        assertThat(updatedStopWords, is(not(arrayWithSize(initialStopWords.length))));
        assertThat(resetStopWords, is(not(arrayWithSize(updatedStopWords.length))));
        assertThat(resetStopWords, is(arrayWithSize(initialStopWords.length)));
        assertThat(resetStopWords, is(equalTo(initialStopWords)));
    }

    @Test
    @DisplayName("Test reset ranking rules when null value is passed")
    public void testUpdateRankingRulesSettingsUsingNull() throws Exception {
        Index index = createIndex("testUpdateRankingRulesSettingsUsingNull");
        String[] initialRankingRule = index.getRankingRulesSettings();
        String[] newRankingRules = {
            "typo",
            "words",
            "sort",
            "proximity",
            "attribute",
            "exactness",
            "release_date:desc",
            "rank:desc"
        };

        index.waitForTask(index.updateRankingRulesSettings(newRankingRules).getTaskUid());
        String[] newRankingRule = index.getRankingRulesSettings();

        index.waitForTask(index.updateRankingRulesSettings(null).getTaskUid());
        String[] resetRankingRule = index.getRankingRulesSettings();

        assertThat(resetRankingRule, is(not(arrayWithSize(newRankingRule.length))));
        assertThat(resetRankingRule, is(arrayWithSize(initialRankingRule.length)));
        assertThat(resetRankingRule, is(equalTo(initialRankingRule)));
    }

    @Test
    @DisplayName("Test update searchable attributes settings when null is passed")
    public void testUpdateSearchableAttributesSettingssUsingNull() throws Exception {
        Index index = createIndex("testUpdateSearchableAttributesSettingssUsingNull");
        String[] initialSearchableAttributes = index.getSearchableAttributesSettings();
        String[] newSearchableAttributes = {"title", "release_date", "cast"};

        index.waitForTask(
                index.updateSearchableAttributesSettings(newSearchableAttributes).getTaskUid());

        String[] updatedSearchableAttributes = index.getSearchableAttributesSettings();
        index.waitForTask(index.updateSearchableAttributesSettings(null).getTaskUid());
        String[] resetSearchableAttributes = index.getSearchableAttributesSettings();

        assertThat(
                updatedSearchableAttributes,
                is(not(arrayWithSize(initialSearchableAttributes.length))));
        assertThat(
                resetSearchableAttributes,
                is(not(arrayWithSize(updatedSearchableAttributes.length))));
        assertThat(
                resetSearchableAttributes, is(arrayWithSize(initialSearchableAttributes.length)));
        assertThat(resetSearchableAttributes, is(equalTo(initialSearchableAttributes)));
    }

    @Test
    @DisplayName("Test update display attributes settings when null is passed")
    public void testUpdateDisplayedAttributesSettingsUsingNull() throws Exception {
        Index index = createIndex("testUpdateDisplayedAttributesSettingsUsingNull");
        String[] initialDisplayedAttributes = index.getDisplayedAttributesSettings();
        String[] newDisplayedAttributes = {"title", "genre", "release_date"};

        index.waitForTask(
                index.updateDisplayedAttributesSettings(newDisplayedAttributes).getTaskUid());
        String[] updatedDisplayedAttributes = index.getDisplayedAttributesSettings();

        index.waitForTask(index.updateDisplayedAttributesSettings(null).getTaskUid());
        String[] resetDisplayedAttributes = index.getDisplayedAttributesSettings();

        assertThat(
                updatedDisplayedAttributes,
                is(not(arrayWithSize(initialDisplayedAttributes.length))));
        assertThat(
                resetDisplayedAttributes,
                is(not(arrayWithSize(updatedDisplayedAttributes.length))));
        assertThat(resetDisplayedAttributes, is(arrayWithSize(initialDisplayedAttributes.length)));
        assertThat(resetDisplayedAttributes, is(equalTo(initialDisplayedAttributes)));
    }

    @Test
    @DisplayName("Test update filterable attributes settings when null is passed")
    public void testUpdateFilterableAttributesSettingsUsingNull() throws Exception {
        Index index = createIndex("testUpdateFilterableAttributesSettingsUsingNull");
        String[] initialFilterableAttributes = index.getFilterableAttributesSettings();
        String[] newFilterableAttributes = {"title", "genres", "cast", "release_date"};

        index.waitForTask(
                index.updateFilterableAttributesSettings(newFilterableAttributes).getTaskUid());
        String[] updatedFilterableAttributes = index.getFilterableAttributesSettings();

        index.waitForTask(index.updateFilterableAttributesSettings(null).getTaskUid());
        String[] resetFilterableAttributes = index.getFilterableAttributesSettings();

        assertThat(
                resetFilterableAttributes,
                is(not(arrayWithSize(updatedFilterableAttributes.length))));
        assertThat(
                updatedFilterableAttributes,
                is(not(arrayWithSize(initialFilterableAttributes.length))));
        assertThat(
                resetFilterableAttributes, is(arrayWithSize(initialFilterableAttributes.length)));
        assertThat(resetFilterableAttributes, is(equalTo(initialFilterableAttributes)));
    }

    @Test
    @DisplayName("Test update distinct attribute settings when null is passed")
    public void testUpdateDistinctAttributeSettingsUsingNull() throws Exception {
        Index index = createIndex("testUpdateDistinctAttributeSettingsUsingNull");
        String initialDistinctAttribute = index.getDistinctAttributeSettings();
        String newDistinctAttribute = "genres";

        index.waitForTask(index.updateDistinctAttributeSettings(newDistinctAttribute).getTaskUid());
        String updatedDistinctAttribute = index.getDistinctAttributeSettings();

        index.waitForTask(index.updateDistinctAttributeSettings(null).getTaskUid());
        String resetDistinctAttribute = index.getDistinctAttributeSettings();

        assertThat(resetDistinctAttribute, is(not(equalTo(updatedDistinctAttribute))));
        assertThat(updatedDistinctAttribute, is(not(equalTo(initialDistinctAttribute))));
        assertThat(resetDistinctAttribute, is(equalTo(initialDistinctAttribute)));
    }

    /** Tests of the pagination setting methods */
    @Test
    @DisplayName("Test get pagination settings by uid")
    public void testGetPaginationSettings() throws Exception {
        Index index = createIndex("testGetPaginationSettings");
        Settings initialSettings = index.getSettings();
        Pagination initialPagination = index.getPaginationSettings();

        assertThat(initialSettings.getPagination().getMaxTotalHits(), is(equalTo(1000)));
        assertThat(initialPagination.getMaxTotalHits(), is(notNullValue()));
    }

    @Test
    @DisplayName("Test update pagination settings")
    public void testUpdatePaginationSettings() throws Exception {
        Index index = createIndex("testUpdatePaginationSettings");
        Pagination newPagination = new Pagination();

        int MaxTotalHitsTypos = 100;

        newPagination.setMaxTotalHits(MaxTotalHitsTypos);
        index.waitForTask(index.updatePaginationSettings(newPagination).getTaskUid());
        Pagination updatedPagination = index.getPaginationSettings();

        assertThat(updatedPagination.getMaxTotalHits(), is(equalTo(100)));
    }

    @Test
    @DisplayName("Test reset pagination settings")
    public void testResetPaginationSettings() throws Exception {
        Index index = createIndex("testResetPaginationSettings");

        Pagination initialPagination = index.getPaginationSettings();
        Pagination newPagination = new Pagination();

        int MaxTotalHitsTypos = 100;
        newPagination.setMaxTotalHits(MaxTotalHitsTypos);
        index.waitForTask(index.updatePaginationSettings(newPagination).getTaskUid());
        Pagination updatedPagination = index.getPaginationSettings();

        index.waitForTask(index.resetPaginationSettings().getTaskUid());
        Pagination paginationAfterReset = index.getPaginationSettings();

        assertThat(initialPagination.getMaxTotalHits(), is(equalTo(1000)));
        assertThat(updatedPagination.getMaxTotalHits(), is(equalTo(100)));
        assertThat(paginationAfterReset.getMaxTotalHits(), is(equalTo(1000)));
    }

    /** Tests of the faceting setting methods */
    @Test
    @DisplayName("Test get faceting settings by uid")
    public void testGetFacetingSettings() throws Exception {
        Index index = createIndex("testGetFacetingSettings");
        Settings initialSettings = index.getSettings();
        Faceting initialFaceting = index.getFacetingSettings();

        assertThat(initialSettings.getFaceting().getMaxValuesPerFacet(), is(equalTo(100)));
        assertThat(initialFaceting.getMaxValuesPerFacet(), is(notNullValue()));
    }

    @Test
    @DisplayName("Test update faceting settings")
    public void testUpdateFacetingSettings() throws Exception {
        Index index = createIndex("testUpdateFacetingSettings");
        Faceting newFaceting = new Faceting();

        int MaxValuesPerFacetTypos = 200;

        newFaceting.setMaxValuesPerFacet(MaxValuesPerFacetTypos);
        index.waitForTask(index.updateFacetingSettings(newFaceting).getTaskUid());
        Faceting updatedFaceting = index.getFacetingSettings();

        assertThat(updatedFaceting.getMaxValuesPerFacet(), is(equalTo(200)));
    }

    @Test
    @DisplayName("Test reset faceting settings")
    public void testResetFacetingSettings() throws Exception {
        Index index = createIndex("testResetFacetingSettings");

        Faceting initialFaceting = index.getFacetingSettings();
        Faceting newFaceting = new Faceting();

        int MaxValuesPerFacetTypos = 200;
        newFaceting.setMaxValuesPerFacet(MaxValuesPerFacetTypos);
        index.waitForTask(index.updateFacetingSettings(newFaceting).getTaskUid());
        Faceting updatedFaceting = index.getFacetingSettings();

        index.waitForTask(index.resetFacetingSettings().getTaskUid());
        Faceting facetingAfterReset = index.getFacetingSettings();

        assertThat(initialFaceting.getMaxValuesPerFacet(), is(equalTo(100)));
        assertThat(updatedFaceting.getMaxValuesPerFacet(), is(equalTo(200)));
        assertThat(facetingAfterReset.getMaxValuesPerFacet(), is(equalTo(100)));
    }

    private Index createIndex(String indexUid) throws Exception {
        Index index = client.index(indexUid);
        TaskInfo updateInfo = index.addDocuments(testData.getRaw());
        index.waitForTask(updateInfo.getTaskUid());

        return index;
    }
}
