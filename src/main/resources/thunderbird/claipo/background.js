browser.composeAction.onClicked.addListener(async (tab) => {
    try {
        // Get the compose window details
        const composeDetails = await browser.compose.getComposeDetails(tab.id);

        const body = composeDetails.isPlainText
            ? composeDetails.plainTextBody
            : composeDetails.body;

        const recipients = composeDetails.to; // Array of recipient strings

        await fetch("http://localhost:8978/claipo", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                body: body,
                recipients: recipients
            })
        });

    } catch (e) {
        console.error("Error calling Claipo:", e);
    }
});