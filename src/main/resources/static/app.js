// JavaJudge front end: plain JavaScript, no build step.

const DEFAULT_CODE = `import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int a = sc.nextInt();
        int b = sc.nextInt();
        System.out.println(a + b);
    }
}
`;

const VERDICT_LABEL = {
  PASSED: "Passed",
  WRONG_ANSWER: "Wrong answer",
  RUNTIME_ERROR: "Runtime error",
  TIME_LIMIT_EXCEEDED: "Time limit",
  OUTPUT_LIMIT_EXCEEDED: "Output limit",
};

const $ = (id) => document.getElementById(id);
const codeBox = $("code");
const testsBox = $("tests");
const resultsBox = $("results");
const runBtn = $("run");
const statusEl = $("status");
const scoreEl = $("score");
let problems = [];

// ---------- test case rows ----------

function addTest(input = "", expected = "") {
  const row = $("test-row").content.firstElementChild.cloneNode(true);
  row.querySelector(".test-input").value = input;
  row.querySelector(".test-expected").value = expected;
  row.querySelector(".remove").addEventListener("click", () => {
    row.remove();
    renumber();
  });
  testsBox.appendChild(row);
  renumber();
}

function renumber() {
  testsBox.querySelectorAll(".test-num").forEach((el, i) => (el.textContent = i + 1));
}

function readTests() {
  return [...testsBox.querySelectorAll(".test-row")].map((row) => ({
    input: row.querySelector(".test-input").value,
    expected: row.querySelector(".test-expected").value,
  }));
}

function setTests(list) {
  testsBox.innerHTML = "";
  list.forEach((t) => addTest(t.input, t.expected));
}

// ---------- problems ----------

async function loadProblems() {
  try {
    const res = await fetch("api/problems");
    problems = await res.json();
    const select = $("problem-select");
    problems.forEach((p) => {
      const opt = document.createElement("option");
      opt.value = p.id;
      opt.textContent = `${p.title} (${p.difficulty})`;
      select.appendChild(opt);
    });
  } catch {
    // the page still works for custom runs
  }
}

function showProblem(id) {
  const p = problems.find((x) => x.id === id);
  $("problem-info").hidden = !p;
  $("custom-info").hidden = !!p;
  clearResults();
  if (!p) {
    codeBox.value = DEFAULT_CODE;
    // The second test overflows int on purpose, so the debugging coach has something to show
    setTests([
      { input: "2 3", expected: "5" },
      { input: "2000000000 2000000000", expected: "4000000000" },
    ]);
    return;
  }
  $("problem-title").textContent = p.title;
  $("problem-difficulty").textContent = p.difficulty;
  $("problem-statement").textContent = p.statement;
  codeBox.value = p.starterCode;
  setTests(p.testCases);
}

// ---------- running ----------

async function run() {
  const tests = readTests();
  if (!codeBox.value.trim()) return setStatus("Write some code first.");
  if (tests.length === 0) return setStatus("Add at least one test case.");

  runBtn.disabled = true;
  setStatus("Compiling and running...");
  const started = performance.now();
  try {
    const res = await fetch("api/run", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code: codeBox.value, testCases: tests }),
    });
    const data = await res.json();
    if (!res.ok) {
      showError(data.error || `Request failed (${res.status})`);
    } else {
      showReport(data);
    }
    setStatus(`Done in ${((performance.now() - started) / 1000).toFixed(1)} s`);
  } catch (e) {
    showError("Could not reach the server.");
    setStatus("");
  } finally {
    runBtn.disabled = false;
  }
}

function setStatus(text) {
  statusEl.textContent = text;
}

function clearResults() {
  scoreEl.textContent = "";
  scoreEl.className = "score";
  resultsBox.innerHTML = '<p class="empty">Run your code to see a verdict for each test case.</p>';
}

function showError(message) {
  scoreEl.textContent = "";
  resultsBox.innerHTML = "";
  const box = el("div", "compile-error");
  box.append(el("strong", "", "Error"), el("pre", "", message));
  resultsBox.appendChild(box);
}

function showReport(report) {
  resultsBox.innerHTML = "";

  if (!report.compiled) {
    scoreEl.textContent = `0 / ${report.total}`;
    scoreEl.className = "score some";
    const box = el("div", "compile-error");
    box.append(el("strong", "", "Compilation error"));
    if (report.compileHints && report.compileHints.length) box.append(hintList(report.compileHints));
    const raw = el("details", "raw");
    raw.append(el("summary", "", "Full javac output"), el("pre", "", report.compileError));
    box.append(raw);
    resultsBox.appendChild(box);
    return;
  }

  scoreEl.textContent = `${report.passed} / ${report.total} passed`;
  scoreEl.className = "score " + (report.passed === report.total ? "all" : "some");

  if (report.summaryHints && report.summaryHints.length) {
    const box = el("div", "coach summary");
    box.append(el("span", "coach-label", "Debugging coach"), hintList(report.summaryHints));
    resultsBox.appendChild(box);
  }

  const firstFail = report.results.find((r) => r.verdict !== "PASSED");
  report.results.forEach((r) => {
    const kind = r.verdict === "PASSED" ? "pass"
      : r.verdict === "WRONG_ANSWER" ? "fail" : "warn";
    const card = el("div", `case ${kind}`);

    const head = el("div", "case-head");
    const meta = el("div", "case-meta");
    meta.append(el("span", "", `${r.timeMs} ms`), el("span", "verdict", VERDICT_LABEL[r.verdict] || r.verdict));
    head.append(el("strong", "", `Test ${r.caseNumber}`), meta);

    const body = el("div", "case-body");
    body.hidden = r !== firstFail; // open only the first red test; click a header to open others
    if (r.hints && r.hints.length) {
      const coach = el("div", "coach");
      coach.append(el("span", "coach-label", "Debugging coach"), hintList(r.hints));
      body.append(coach);
    }
    if (r.diff) body.append(diffBlock(r.diff));
    body.append(block("Input", r.input), block("Expected", r.expected), block("Your output", r.actual));
    if (r.error) {
      const raw = el("details", "raw");
      raw.open = !(r.hints && r.hints.length);
      raw.append(el("summary", "", "Full error"), el("pre", "", r.error));
      body.append(raw);
    }

    head.addEventListener("click", () => (body.hidden = !body.hidden));
    card.append(head, body);
    resultsBox.appendChild(card);
  });
}

function hintList(hints) {
  const list = el("ul", "hints");
  hints.forEach((h) => {
    const item = el("li");
    const title = el("strong", "", h.title);
    item.append(title);
    if (h.line) {
      const btn = el("button", "line-link", `line ${h.line}`);
      btn.type = "button";
      btn.addEventListener("click", (e) => {
        e.stopPropagation();
        jumpToLine(h.line);
      });
      item.append(btn);
    }
    item.append(el("p", "", h.detail));
    list.append(item);
  });
  return list;
}

function diffBlock(d) {
  const wrap = el("div", "diff");
  wrap.append(el("span", "", `First difference: line ${d.line}`));
  const grid = el("div", "diff-grid");
  grid.append(
    el("span", "diff-key", "Expected"), el("code", "diff-exp", d.expected ?? "(no line)"),
    el("span", "diff-key", "Yours"), el("code", "diff-act", d.actual ?? "(no line)"),
  );
  wrap.append(grid);
  return wrap;
}

/** Selects a line in the code box so the user sees where the hint points. */
function jumpToLine(n) {
  const lines = codeBox.value.split("\n");
  let start = 0;
  for (let i = 0; i < n - 1 && i < lines.length; i++) start += lines[i].length + 1;
  const end = start + (lines[n - 1] || "").length;
  codeBox.focus();
  codeBox.setSelectionRange(start, end);
  const lineHeight = parseFloat(getComputedStyle(codeBox).lineHeight) || 20;
  codeBox.scrollTop = Math.max(0, (n - 4) * lineHeight);
}

function block(label, text) {
  const wrap = el("div");
  wrap.append(el("span", "", label), el("pre", "", text === "" || text == null ? "(empty)" : text));
  return wrap;
}

function el(tag, className = "", text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text; // textContent, never innerHTML: output is untrusted
  return node;
}

// ---------- editor helpers ----------

codeBox.addEventListener("keydown", (e) => {
  if (e.key === "Tab") {
    e.preventDefault();
    const { selectionStart: s, selectionEnd: end } = codeBox;
    codeBox.setRangeText("    ", s, end, "end");
  }
  if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
    e.preventDefault();
    run();
  }
});

$("file-input").addEventListener("change", async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  if (file.size > 64 * 1024) return setStatus("File is larger than 64 KB.");
  codeBox.value = await file.text();
  setStatus(`Loaded ${file.name}`);
  e.target.value = "";
});

$("add-test").addEventListener("click", () => addTest());
$("problem-select").addEventListener("change", (e) => showProblem(e.target.value));
runBtn.addEventListener("click", run);

// ---------- start ----------

showProblem("");
loadProblems();
