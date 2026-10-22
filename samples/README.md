# Sample files for testing the compliance checker

| File | Upload as | Expected score |
|---|---|---|
| `GDPR_example_excerpt.txt` | Official law (Admin page, or copy into `docs/`) | (the law) |
| `Policy_A_Office_IT_Rules_low.txt` | My Documents | low, about 0-20 % |
| `Policy_B_Data_Protection_Basics_medium.txt` | My Documents | medium, about 40-60 % |
| `Policy_C_Full_Data_Protection_Programme_high.txt` | My Documents | high, about 90-100 % |

The GDPR file is a shortened, paraphrased excerpt of 18 articles of Regulation (EU) 2016/679 that set
obligations for organisations. It is made for testing, not the official text.

## Steps

1. Log in as admin and upload `GDPR_example_excerpt.txt` on the **Admin** page (or copy it into `docs/` and
   restart the backend). Wait until it shows "Ready".
2. On the Admin page, click **Generate checklist** for it and wait until the checklist is ready.
3. On **My Documents**, upload the three policy files and wait until they are indexed.
4. On **Compliance Check**, choose a policy and the GDPR excerpt, and click **Run check**. Repeat for each policy.

## Notes

- The scores are targets, not exact numbers: Gemini writes the checklist and judges each requirement, so
  results can vary by a few percent between runs or after regenerating the checklist.
- Quota: generating the checklist is about 1 Gemini call. Each check needs one search per requirement plus
  about one call per 8 requirements, so about 20-30 requests in total. Run the checks before a presentation,
  not during it.
