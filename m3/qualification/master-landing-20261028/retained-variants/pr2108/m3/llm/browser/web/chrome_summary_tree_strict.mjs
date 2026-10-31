import {
  runSummaryTree as baseRunSummaryTree,
  validateSummaryTreePlan as baseValidateSummaryTreePlan,
} from "./chrome_summary_tree.mjs";
import { verifySummaryTreeIntegrity } from "./chrome_summary_tree_integrity.mjs";

export async function validateSummaryTreePlan(plan, sourceText) {
  await verifySummaryTreeIntegrity(plan, sourceText);
  return baseValidateSummaryTreePlan(plan, sourceText);
}

export async function runSummaryTree(plan, sourceText, options = {}, summarizeNode = undefined) {
  await validateSummaryTreePlan(plan, sourceText);
  return baseRunSummaryTree(plan, sourceText, options, summarizeNode);
}
