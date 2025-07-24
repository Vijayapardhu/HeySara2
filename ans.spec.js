import { test, expect } from '@playwright/test';

const API_URL = 'https://api-inference.huggingface.co/models/deepset/roberta-base-squad2';
const API_KEY = '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';

// Helper to build prompt for AI
function buildPrompt(texts) {
    let question = '';
    const options = [];
    for (const t of texts) {
        if (/^\d+\)?/.test(t.trim())) {
            options.push(t.trim());
        } else {
            question += t.trim() + ' ';
        }
    }
    let prompt = `Question: ${question.trim()}\nOptions:\n`;
    for (const opt of options) prompt += opt + '\n';
    prompt += 'Analyze the question and only reply with the correct answer text, not the letter or explanation.';
    return prompt;
}

async function getAIAnswer(questionText, optionLabels) {
    const apiUrl = "https://openrouter.ai/api/v1/chat/completions";
    const apiKey = "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";
    const prompt = `You are an expert at multiple choice questions.
Choose the correct answer from the options below.
Reply ONLY with the exact correct option text, and nothing else.
If you don't know, reply with the most likely option.
Question: ${questionText}
Options:
${optionLabels.join('\n')}
Correct answer:`;

    const body = {
        model: "deepseek/deepseek-r1-0528-qwen3-8b:free",
        messages: [
            { role: "user", content: prompt }
        ]
    };

    try {
        const response = await fetch(apiUrl, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${apiKey}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(body)
        });
        const result = await response.json();
        return result.choices?.[0]?.message?.content?.trim() || null;
    } catch (err) {
        console.log("AI API Error:", err.message);
        return null;
    }
}

test('Automate Maya TechnicalHub test with AI answering', async ({ page, request, browser }) => {
    test.setTimeout(120000); // Set timeout to 2 minutes

    // Step 1: Go to sign-in page
    await page.goto('https://maya.technicalhub.io/sign-in?slug=aptmandatory');

    // Step 2: Fill in credentials and sign in
    await page.fill('input[name="roll_no"]', '23404CM088');
    await page.fill('input[name="password"]', 'Pardhu@2008');
    await page.click('button:has-text("Sign In")');
    await page.click('button:has-text("Yes")');
    await page.waitForNavigation();

    // Step 3: Click "Learn More" for Level-1
    await page.click('a[href*="apt-mandatory-tests-list/66a0d5973001b8b3a2ae5f4a"]');
    await page.waitForNavigation();

    // Step 4: Click refresh if present
    const refreshBtn = await page.$('button:has-text("Refresh")');
    if (refreshBtn) {
        await refreshBtn.click();
    }

    // Step 5: Click the first "Take" button
    await page.waitForSelector('button:has-text("Take")');
    await page.click('button:has-text("Take")');

    // Step 6: Click "Start" on instructions page
    await page.waitForSelector('button:has-text("Start")');
    await page.click('button:has-text("Start")');

    // Step 7+: Loop through all questions
    while (true) {
        try {
            await page.waitForSelector('div.blog-details-content h5 span', { timeout: 15000 });
        } catch {
            console.log('No more questions found, exiting loop.');
            break;
        }

        const questionText = await page.textContent('div.blog-details-content h5 span');
        const optionLabels = await page.$$eval('form .form-check-label span', spans => spans.map(s => s.textContent?.trim() || ''));

        if (!questionText || optionLabels.length === 0) {
            console.log('No question or options found, exiting loop.');
            break;
        }

        console.log('Question:', questionText);
        console.log('Options:', optionLabels);

        // Get AI answer with timeout
        let aiAnswer = null;
        try {
            aiAnswer = await Promise.race([
                getAIAnswer(questionText, optionLabels),
                new Promise((_, reject) => setTimeout(() => reject(new Error('AI timeout')), 15000))
            ]);
        } catch (err) {
            console.log('AI error or timeout:', err.message);
        }
        console.log('AI Answer:', aiAnswer);

        // Select the matching option
        let matched = false;
        for (let i = 0; i < optionLabels.length; i++) {
            if (optionLabels[i].toLowerCase() === aiAnswer?.toLowerCase()) {
                await page.click(`input.form-check-input[value="${optionLabels[i]}"]`);
                matched = true;
                break;
            }
            
        }
        if (!matched && optionLabels.length > 0) {
            await page.click(`input.form-check-input[value="${optionLabels[0]}"]`);
        }

        // Try to click "Next"
        const nextBtn = await page.$('button:has-text("Next")');
        if (nextBtn) {
            await nextBtn.click();
            await page.waitForTimeout(1000);
        } else {
            console.log('No Next button, exiting question loop.');
            break;
        }
    }

    // After questions, click "Submit" every 30 seconds until it disappears
    while (true) {
        const submitBtn = await page.$('button:has-text("Submit")');
        if (submitBtn) {
            await submitBtn.click();
            console.log('Clicked Submit, waiting 30 seconds...');
            await page.waitForTimeout(30000);
        } else {
            break;
        }
    }
});