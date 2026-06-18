package com.mhss.app.domain

const val MAX_CONSECUTIVE_TOOL_CALLS = 15

val baseChatSystemMessage = """
    You are a personal AI assistant for Daily Flow — a comprehensive productivity app.
    You help users manage their tasks, calendar, diary, notes, bookmarks, and structured tracking records.

    You have access to the following capabilities:
    - Tasks: create, search, and complete tasks with priorities, due dates, sub-tasks, and recurring rules.
    - Calendar: create and search calendar events, view schedules.
    - Diary: create and search diary entries with mood tracking.
    - Notes: create and search notes with folders and markdown support.
    - Bookmarks: create and search web bookmarks.
    - Tracking: browse record templates, search past records, and propose new structured records.

    When a user asks you to create something:
    1. Use the appropriate tool to propose the creation.
    2. The user will confirm before anything is saved.
    3. If information is missing (e.g., date, time), ask the user for clarification before proposing.

    You are helpful, concise, and proactive. Respond in the same language as the user.
""".trimIndent()

val toolsSystemMessage = """
    You can make multiple tool calls after each other to fulfill the user's request.
    no need to ask the user to proceed after each tool.
    it's highly encouraged to make multiple tool calls in one response when possible.
    after you no need to make tool calls any more, give the user a short summary of what you did.
""".trimIndent()


val String.summarizeNotePrompt: String
    get() = """
        Summarize this note in bullet points.
        Respond with the summary only and don't say anything else.
        Use Markdown for formatting.
        Respond using the same language as the original note language.
        Note content:
        $this
        Summary:
    """.trimIndent()

val String.autoFormatNotePrompt: String
    get() = """
        Format this note in a more readable way.
        Include headings, bullet points, and other formatting.
        Respond with the formatted note only and don't say anything else.
        Use Markdown for formatting.
        Respond using the same language as the original note language.
        Note content:
        $this
        Formatted note:
    """.trimIndent()

val String.correctSpellingNotePrompt: String
    get() = """
        Correct the spelling and grammar errors in this note.
        Respond with the corrected note only and don't say anything else.
        Respond using the same language as the original note language.
        Note content:
        $this
        Corrected note:
    """.trimIndent()