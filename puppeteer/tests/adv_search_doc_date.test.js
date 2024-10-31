import { CodexPage } from '../framework/codex_page';
import { afterEach, beforeEach, describe, test } from "@jest/globals";

describe('Filing Index', () => {
    let codexPage;

    beforeEach(async () => {
        codexPage = new CodexPage();
        await codexPage.buildPage();
    })

    afterEach(async () => {
        await codexPage.tearDown();
    })

    test('Advanced Search - Document Date', async () => {
        await codexPage.navigateToFilingIndex();
        await codexPage.search.companyNameInput.enterText('TUSCANY PIZZA LTD');
        await codexPage.search.companyNumberInput.enterText('11162569');
        await codexPage.search.advancedSearchToggle.select();

        // Set the minimum doc date
        await codexPage.search.advancedSearch.minDocDateYear.scrollToElement();
        await codexPage.search.advancedSearch.minDocDateYear.enterText('2023');
        await codexPage.search.advancedSearch.minDocDateMonth.enterText('1');
        await codexPage.search.advancedSearch.minDocDateDay.enterText('31');

        // Set the maximum doc date
        await codexPage.search.advancedSearch.maxDocDateYear.enterText('2023');
        await codexPage.search.advancedSearch.maxDocDateMonth.enterText('1');
        await codexPage.search.advancedSearch.maxDocDateDay.enterText('31');

        await codexPage.search.submitButton.select();

        // Assert search results
        await codexPage.search.assertResultCount(1);
        const result = await codexPage.search.getSearchResult('TUSCANY PIZZA LTD',
            '11162569', '2023-01-31', '2023-06-02');
        await result.filingButton.scrollToElement();
        await result.filingButton.assertVisible();

        // Open Viewer
        await result.viewerButton.scrollToElement();
        await result.viewerButton.select();
        await codexPage.assertPageNavigation('iXBRL Viewer');

    });
});
