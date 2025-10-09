package it.atraj.habittracker.email

import it.atraj.habittracker.data.local.Habit
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Beautiful, responsive HTML email templates for habit notifications.
 * Compatible with mobile Gmail and other email clients.
 */
object EmailTemplate {
    
    /**
     * Generate a habit reminder email with a direct link to the habit details.
     * Note: Gmail blocks JavaScript and form submissions, so we use deep links instead.
     */
    fun generateHabitReminderEmail(
        habit: Habit,
        userName: String?,
        deepLink: String
    ): String {
        val time = LocalTime.of(habit.reminderHour, habit.reminderMinute)
        val timeStr = time.format(DateTimeFormatter.ofPattern("h:mm a"))
        val displayName = userName ?: "there"
        val habitEmoji = habit.avatar.value
        
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Habit Reminder - ${habit.title}</title>
    <!--[if mso]>
    <style type="text/css">
        body, table, td {font-family: Arial, Helvetica, sans-serif !important;}
    </style>
    <![endif]-->
</head>
<body style="margin: 0; padding: 0; background-color: #f5f5f5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="background-color: #f5f5f5;">
        <tr>
            <td style="padding: 20px 0;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                    
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px 20px; text-align: center;">
                            <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 700; text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);">
                                🎯 Habit Reminder
                            </h1>
                        </td>
                    </tr>
                    
                    <!-- Greeting -->
                    <tr>
                        <td style="padding: 30px 30px 20px;">
                            <p style="margin: 0 0 10px; color: #333333; font-size: 18px; font-weight: 600;">
                                Hi $displayName! 👋
                            </p>
                            <p style="margin: 0; color: #666666; font-size: 16px; line-height: 1.5;">
                                It's time for your habit:
                            </p>
                        </td>
                    </tr>
                    
                    <!-- Habit Card -->
                    <tr>
                        <td style="padding: 0 30px 20px;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="background-color: #f8f9fa; border-radius: 8px; border-left: 4px solid #667eea;">
                                <tr>
                                    <td style="padding: 20px;">
                                        <h2 style="margin: 0 0 8px; color: #667eea; font-size: 22px; font-weight: 700;">
                                            ${habit.title}
                                        </h2>
                                        ${if (habit.description.isNotBlank()) """
                                        <p style="margin: 0 0 12px; color: #666666; font-size: 15px; line-height: 1.5;">
                                            ${habit.description}
                                        </p>
                                        """ else ""}
                                        <div style="display: inline-block; background-color: #e3e7ff; color: #667eea; padding: 6px 12px; border-radius: 4px; font-size: 14px; font-weight: 600;">
                                            ⏰ $timeStr
                                        </div>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- Call to Action -->
                    <tr>
                        <td style="padding: 10px 30px 30px; text-align: center;">
                            <p style="margin: 0 0 20px; color: #666666; font-size: 15px;">
                                Ready to complete this habit?
                            </p>
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin: 0 auto;">
                                <tr>
                                    <td style="border-radius: 8px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);">
                                        <a href="$deepLink" target="_blank" style="display: inline-block; padding: 16px 40px; font-size: 16px; font-weight: 700; color: #ffffff; text-decoration: none; text-transform: uppercase; letter-spacing: 0.5px;">
                                            Open Habit Tracker
                                        </a>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- Divider -->
                    <tr>
                        <td style="padding: 0 30px;">
                            <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 0;">
                        </td>
                    </tr>
                    
                    <!-- Motivation Quote -->
                    <tr>
                        <td style="padding: 20px 30px; background-color: #fafbfc;">
                            <p style="margin: 0; color: #999999; font-size: 14px; font-style: italic; text-align: center; line-height: 1.6;">
                                💪 "Success is the sum of small efforts, repeated day in and day out."
                            </p>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="padding: 20px 30px 30px; text-align: center;">
                            <p style="margin: 0 0 10px; color: #999999; font-size: 13px;">
                                This is an automated reminder from <strong>Habit Tracker</strong>
                            </p>
                            <p style="margin: 0; color: #999999; font-size: 12px;">
                                You can disable email notifications in the app settings
                            </p>
                        </td>
                    </tr>
                    
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
        """.trimIndent()
    }
    
    /**
     * Generate plain text version for email clients that don't support HTML
     */
    fun generatePlainTextEmail(
        habit: Habit,
        userName: String?,
        deepLink: String
    ): String {
        val time = LocalTime.of(habit.reminderHour, habit.reminderMinute)
        val timeStr = time.format(DateTimeFormatter.ofPattern("h:mm a"))
        val displayName = userName ?: "there"
        
        return """
🎯 HABIT REMINDER

Hi $displayName! 👋

It's time for your habit:

📌 ${habit.title}
${if (habit.description.isNotBlank()) "📝 ${habit.description}\n" else ""}
⏰ Scheduled for: $timeStr

Ready to complete this habit? Open the app:
$deepLink

💪 "Success is the sum of small efforts, repeated day in and day out."

---
This is an automated reminder from Habit Tracker
You can disable email notifications in the app settings
        """.trimIndent()
    }
    
    /**
     * Generate email subject line
     */
    fun generateSubject(habit: Habit): String {
        return "🎯 Time for: ${habit.title}"
    }
}

