const api = (path) => `${location.origin}/api${path}`;

const state = {
  user: JSON.parse(localStorage.getItem("sp.v1.user") || "null"),
  token: localStorage.getItem("sp.v1.token") || "",
  view: "dashboard",
  users: [],
  roles: [],
  projects: [],
  project: null,
  projectMode: "view",
  modelProviders: [],
  modelProfiles: [],
  ima: null,
  imaTestResult: null,
  modelTestResult: null,
  editingModelId: null,
  editingSkillId: null,
  knowledgeScopes: [],
  skills: [],
};

function workflowTasks() {
  return state.skills
    .filter((s) => s.enabled && (s.category || "WORKFLOW") === "WORKFLOW")
    .sort((a, b) => Number(a.sort_order || 100) - Number(b.sort_order || 100))
    .map((s) => ({
      code: s.code,
      label: s.name,
      owner: s.description || s.code,
      asset: assetKeyForOutput(s.output_type, s.code),
      outputType: s.output_type || "GENERIC",
    }));
}

function assetKeyForOutput(outputType, code) {
  const map = {
    REQUIREMENT: "requirementAnalyses",
    PRODUCT: "productMatches",
    CASE: "caseMatches",
    ARCHITECTURE: "architectures",
    PROPOSAL: "proposalSections",
    PPT: "pptPages",
    QA: "agentRuns",
  };
  return map[outputType] || (code === "qa" ? "agentRuns" : "agentRuns");
}

function parseMaybeJson(value, fallback = {}) {
  if (!value) return fallback;
  if (typeof value === "object" && typeof value.value === "string") return parseMaybeJson(value.value, fallback);
  if (typeof value === "object") return value;
  try {
    return JSON.parse(value);
  } catch {
    return fallback;
  }
}

async function request(path, options = {}) {
  const res = await fetch(api(path), {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}),
      ...(options.headers || {}),
    },
  });
  const body = await res.json();
  if (!res.ok || body.success === false) throw new Error(body.message || "请求失败");
  return body.data;
}

function isAdmin() {
  return (state.user?.roles || []).includes("ADMIN");
}

function canProjectWrite() {
  return isAdmin() || (state.user?.roles || []).includes("WORKER");
}

function roleLabel(code) {
  if (code === "ADMIN") return "管理员";
  if (code === "WORKER") return "牛马专用";
  return code || "-";
}

function statusLabel(code) {
  const labels = { DRAFT: "草稿", GENERATED: "已生成", REVIEW: "待审核", DONE: "已完成" };
  return labels[code] || code || "-";
}

function statusClass(code) {
  if (code === "DONE") return "ok";
  if (code === "GENERATED" || code === "REVIEW") return "warn";
  return "";
}

async function boot() {
  if (!state.user) {
    renderLogin();
    return;
  }
  await loadBase();
  renderApp();
}

async function loadBase() {
  const [users, roles, projects, modelProviders, modelProfiles, ima, knowledgeScopes, skills] = await Promise.all([
    request("/users"),
    request("/users/roles"),
    request("/projects"),
    request("/model-providers"),
    request("/model-profiles"),
    request("/ima-skill"),
    request("/knowledge-scopes"),
    request("/skills"),
  ]);
  Object.assign(state, { users, roles, projects, modelProviders, modelProfiles, ima, knowledgeScopes, skills });
}

function renderLogin() {
  document.getElementById("app").innerHTML = `
    <main class="login">
      <section class="login-main">
        <div class="brand">
          <div class="brand-mark">SP</div>
          <div><h1>SolutionPilot</h1><span>GIS 方案工程平台</span></div>
        </div>
        <form class="form" onsubmit="login(event)">
          <div class="field wide">
            <label>邮箱</label>
            <input id="email" class="input" value="admin@example.com" />
          </div>
          <div class="field wide">
            <label>密码</label>
            <input id="password" class="input" type="password" value="admin" />
          </div>
          <button class="btn primary wide" type="submit">进入系统</button>
        </form>
      </section>
      <section class="login-side">
        <span class="chip">本地 V1</span>
        <h2>从客户需求到方案资产，一套可追踪的 AI 工作台。</h2>
        <p>项目管理、Agent 工作流、大模型配置和 ima 知识库接入都围绕方案交付流程组织。</p>
      </section>
    </main>
    <div id="toast" class="toast"></div>
  `;
}

async function login(event) {
  event.preventDefault();
  const data = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify({
      email: document.getElementById("email").value,
      password: document.getElementById("password").value,
    }),
  });
  state.user = data;
  state.token = data.token;
  localStorage.setItem("sp.v1.user", JSON.stringify(data));
  localStorage.setItem("sp.v1.token", data.token);
  state.view = "dashboard";
  await boot();
}

function logout() {
  localStorage.removeItem("sp.v1.user");
  localStorage.removeItem("sp.v1.token");
  state.user = null;
  state.token = "";
  state.view = "dashboard";
  renderLogin();
}

function renderApp() {
  document.getElementById("app").innerHTML = `
    <div class="app">
      <aside class="sidebar">
        <div class="brand">
          <div class="brand-mark">SP</div>
          <div><h1>SolutionPilot</h1><span>GIS 方案工程平台</span></div>
        </div>
        <div class="user-box">
          <b>${escapeHtml(state.user.name)}</b>
          <span>${(state.user.roles || []).map(roleLabel).join("，")}</span>
        </div>
        <nav class="nav">
          ${nav("dashboard", "首页看板")}
          ${nav("project-list", "项目列表")}
          ${nav("project-new", "新建项目")}
          ${nav("project-manage", "项目工作台")}
          ${isAdmin() ? nav("users", "用户权限") : ""}
          ${isAdmin() ? nav("models", "大模型配置") : ""}
          ${isAdmin() ? nav("ima", "ima Skill") : ""}
          ${isAdmin() ? nav("skills", "Agent Skills") : ""}
          ${isAdmin() ? nav("knowledge", "知识范围") : ""}
        </nav>
        <button class="btn ghost dark-button" onclick="logout()">退出登录</button>
      </aside>
      <main class="content">${renderView()}</main>
    </div>
    <div id="toast" class="toast"></div>
  `;
}

function nav(view, label) {
  const click = view === "project-manage" ? "openLastProject()" : `setView('${view}')`;
  return `<button class="${state.view === view ? "active" : ""}" onclick="${click}">${label}</button>`;
}

async function setView(view) {
  state.view = view;
  if (view !== "project-manage") {
    state.project = null;
    state.projectMode = "view";
  }
  await loadBase();
  renderApp();
}

function renderView() {
  if (state.view === "project-list") return renderProjectList();
  if (state.view === "project-new") return renderProjectForm("create");
  if (state.view === "project-manage") return renderProjectManage();
  if (state.view === "users") return isAdmin() ? renderUsers() : noAccess();
  if (state.view === "models") return isAdmin() ? renderModels() : noAccess();
  if (state.view === "ima") return isAdmin() ? renderIma() : noAccess();
  if (state.view === "skills") return isAdmin() ? renderSkills() : noAccess();
  if (state.view === "knowledge") return isAdmin() ? renderKnowledge() : noAccess();
  return renderDashboard();
}

function header(title, desc, actions = "") {
  return `<div class="topbar"><div><h2>${title}</h2><p>${desc}</p></div><div class="toolbar">${actions}</div></div>`;
}

function noAccess() {
  return `${header("无权限", "当前角色不能访问该页面。")}<section class="panel"><p class="muted">请使用管理员账号登录。</p></section>`;
}

function metric(label, value, desc = "") {
  return `<article class="card metric span-3"><span>${label}</span><strong>${value}</strong>${desc ? `<p>${desc}</p>` : ""}</article>`;
}

function renderDashboard() {
  const running = state.projects.filter((p) => ["DRAFT", "GENERATED", "REVIEW"].includes(p.status)).length;
  const latest = state.projects.slice(0, 4);
  return `
    ${header("首页看板", "查看项目进展、待办任务、模型配置和 ima 接入状态。", `<button class="btn primary" onclick="setView('project-new')">新建方案项目</button>`)}
    <section class="grid">
      ${metric("项目总数", state.projects.length, "全部方案项目")}
      ${metric("进行中", running, "草稿、生成中或待审核")}
      ${metric("模型配置", state.modelProfiles.length, "OpenAI-compatible")}
      ${metric("ima 知识范围", state.knowledgeScopes.filter((x) => x.enabled).length, "启用范围")}
      <div class="panel span-7">
        <div class="panel-title"><div><h3>最近项目</h3><p>点击进入项目工作台</p></div></div>
        <div class="project-grid compact">${latest.map(renderProjectCard).join("") || `<p class="muted">暂无项目。</p>`}</div>
      </div>
      <div class="panel span-5">
        <div class="panel-title"><div><h3>流程状态</h3><p>V1 可串联运行的 Agent</p></div></div>
        <div class="list">${workflowTasks().map((s) => `<div class="list-item"><b>${escapeHtml(s.label)}</b><div class="muted">${escapeHtml(s.owner)}</div></div>`).join("") || `<div class="list-item muted">暂无启用任务。</div>`}</div>
      </div>
    </section>
  `;
}

function renderProjectList() {
  return `
    ${header("项目列表", "搜索、筛选、打开工作台，也可以编辑或删除项目。", `<button class="btn primary" onclick="setView('project-new')">新建项目</button>`)}
    <section class="filters">
      <input id="projectSearch" class="input" placeholder="搜索项目、客户、行业" oninput="filterProjects()" />
      <select id="statusFilter" class="select" onchange="filterProjects()">
        <option value="">全部状态</option>
        <option value="DRAFT">草稿</option>
        <option value="GENERATED">已生成</option>
        <option value="REVIEW">待审核</option>
        <option value="DONE">已完成</option>
      </select>
      <select id="industryFilter" class="select" onchange="filterProjects()">
        <option value="">全部行业</option>
        ${[...new Set(state.projects.map((p) => p.industry).filter(Boolean))].map((x) => `<option>${escapeHtml(x)}</option>`).join("")}
      </select>
    </section>
    <section id="projectGrid" class="project-grid">${state.projects.map(renderProjectCard).join("") || `<div class="panel"><p class="muted">暂无项目。</p></div>`}</section>
  `;
}

function renderProjectCard(p) {
  const search = `${p.name} ${p.customer_name || ""} ${p.industry || ""}`.toLowerCase();
  return `
    <article class="card project-card" data-search="${escapeHtml(search)}" data-status="${escapeHtml(p.status)}" data-industry="${escapeHtml(p.industry || "")}">
      <div class="meta">
        <span class="chip">${escapeHtml(p.industry || "未分类")}</span>
        <span class="chip ${statusClass(p.status)}">${statusLabel(p.status)}</span>
        <span class="chip">${escapeHtml(p.model_profile_name || "默认模型")}</span>
      </div>
      <h3>${escapeHtml(p.name)}</h3>
      <div class="kv">
        ${kv("客户", p.customer_name)}
        ${kv("进度", `${p.progress || 0}%`)}
        ${kv("创建人", p.creator_name || "-")}
        ${kv("更新", formatDate(p.updated_at))}
      </div>
      <div class="progress"><span style="width:${Number(p.progress || 0)}%"></span></div>
      <div class="toolbar">
        <button class="btn primary" onclick="openProject('${p.id}')">打开工作台</button>
        <button class="btn ghost" onclick="editProject('${p.id}')">编辑</button>
        <button class="btn danger" onclick="deleteProject('${p.id}', '${escapeAttr(p.name)}')">删除</button>
      </div>
    </article>
  `;
}

function filterProjects() {
  const q = document.getElementById("projectSearch").value.trim().toLowerCase();
  const status = document.getElementById("statusFilter").value;
  const industry = document.getElementById("industryFilter").value;
  document.querySelectorAll(".project-card").forEach((card) => {
    const okSearch = !q || card.dataset.search.includes(q);
    const okStatus = !status || card.dataset.status === status;
    const okIndustry = !industry || card.dataset.industry === industry;
    card.style.display = okSearch && okStatus && okIndustry ? "" : "none";
  });
}

async function openLastProject() {
  if (state.project) {
    state.view = "project-manage";
    state.projectMode = "view";
    renderApp();
    return;
  }
  if (state.projects[0]) {
    await openProject(state.projects[0].id);
    return;
  }
  await setView("project-new");
}

async function openProject(id) {
  state.project = await request(`/projects/${id}`);
  state.projectMode = "view";
  state.view = "project-manage";
  renderApp();
}

async function editProject(id) {
  state.project = await request(`/projects/${id}`);
  state.projectMode = "edit";
  state.view = "project-manage";
  renderApp();
}

function renderProjectManage() {
  if (!state.project) {
    return `
      ${header("项目工作台", "选择一个项目进入需求分析、产品匹配、架构设计和方案生成流程。", `<button class="btn primary" onclick="setView('project-new')">新建项目</button>`)}
      <section class="project-grid">${state.projects.map(renderProjectCard).join("") || `<div class="panel"><p class="muted">暂无项目。</p></div>`}</section>
    `;
  }
  if (state.projectMode === "edit") return renderProjectForm("edit", state.project);
  const p = state.project;
  return `
    ${header(
      p.name,
      `${escapeHtml(p.customer_name)} · ${escapeHtml(p.industry || "")} · ${statusLabel(p.status)}`,
      `<button class="btn ghost" onclick="setView('project-list')">返回列表</button><button class="btn" onclick="editProject('${p.id}')">编辑项目</button><button class="btn primary" onclick="runPipeline('${p.id}')">一键跑通流程</button>`
    )}
    <section class="workspace">
      <aside class="panel steps">
        <div class="panel-title"><div><h3>Agent 流程</h3><p>单步运行或一键串联</p></div></div>
        <div class="field run-model">
          <label>本次执行模型</label>
          <select id="workspaceModelProfileId" class="select">
            ${state.modelProfiles.map((m) => `<option value="${m.id}" ${String(m.id) === String(p.model_profile_id || "") ? "selected" : ""}>${escapeHtml(m.name)} · ${escapeHtml(m.model_name)} · ${m.status}</option>`).join("")}
          </select>
        </div>
        ${workflowTasks().map((s) => renderStepButton(p, s)).join("") || `<p class="muted">暂无启用任务，请到 Agent Skills 新增或启用任务。</p>`}
      </aside>
      <section class="panel">
        <div class="panel-title"><div><h3>交付资产</h3><p>Agent 运行后写入 PostgreSQL 业务表</p></div></div>
        <div class="grid asset-grid">
          ${asset("运行记录", p.agentRuns?.length || 0)}
          ${asset("需求分析", p.requirementAnalyses?.length || 0)}
          ${asset("产品匹配", p.productMatches?.length || 0)}
          ${asset("案例推荐", p.caseMatches?.length || 0)}
          ${asset("技术架构", p.architectures?.length || 0)}
          ${asset("方案章节", p.proposalSections?.length || 0)}
          ${asset("PPT 页面", p.pptPages?.length || 0)}
        </div>
        ${renderDeliverables(p)}
        ${renderProjectAssets(p)}
      </section>
      <aside class="panel context-panel">
        <div class="panel-title"><div><h3>项目上下文</h3><p>用于 Agent 输入</p></div></div>
        <div class="list">
          <div class="list-item"><b>背景</b><div class="muted">${escapeHtml(p.background || "-")}</div></div>
          <div class="list-item"><b>原始需求</b><div class="muted">${escapeHtml(p.raw_demand || "-")}</div></div>
          <div class="list-item"><b>已有系统</b><div class="muted">${escapeHtml(p.existing_systems || "-")}</div></div>
          <div class="list-item"><b>知识范围</b><div class="muted">${(p.knowledgeScopes || []).map((x) => escapeHtml(x.name)).join("，") || "-"}</div></div>
          <div class="list-item"><b>交付物</b><div class="muted">${(p.deliverables || []).map((x) => escapeHtml(x.deliverable_type)).join("，") || "-"}</div></div>
        </div>
      </aside>
    </section>
  `;
}

function renderStepButton(p, step) {
  const count = taskRunCount(p, step);
  return `
    <button class="step-button" onclick="runAgent('${p.id}', '${step.code}')">
      <span><b>${step.label}</b><small>${step.owner}</small></span>
      <em>${count > 0 ? "已产出" : "未运行"}</em>
    </button>
  `;
}

function taskRunCount(p, step) {
  if (step.asset !== "agentRuns") {
    return Array.isArray(p[step.asset]) ? p[step.asset].length : 0;
  }
  return (p.agentRuns || []).filter((run) => run.skill_code === step.code).length;
}

function asset(label, value) {
  return `<article class="card metric span-3"><span>${label}</span><strong>${value}</strong></article>`;
}

function renderDeliverables(p) {
  const generated = (p.proposalSections?.length || 0) + (p.pptPages?.length || 0) + (p.architectures?.length || 0);
  return `
    <section class="deliverables">
      <div class="panel-title"><div><h3>交付物下载</h3><p>${generated > 0 ? "可下载当前已生成内容" : "先运行 Agent 流程生成方案内容"}</p></div></div>
      <div class="deliverable-grid">
        ${deliverableButton("方案 Markdown", "完整方案正文", "downloadMarkdown", generated > 0)}
        ${deliverableButton("Word 兼容文档", "可用 Word 打开的 .doc", "downloadWord", generated > 0)}
        ${deliverableButton("PPT 大纲", "页面标题、类型和要点", "downloadPptMarkdown", (p.pptPages?.length || 0) > 0)}
        ${deliverableButton("架构图代码", "Mermaid 架构图文本", "downloadMermaid", (p.architectures?.length || 0) > 0)}
        ${deliverableButton("项目上下文 JSON", "完整项目和生成资产", "downloadJson", true)}
      </div>
    </section>
  `;
}

function deliverableButton(title, desc, action, enabled) {
  return `
    <button class="download-card" onclick="${enabled ? `${action}()` : ""}" ${enabled ? "" : "disabled"}>
      <b>${title}</b>
      <span>${desc}</span>
    </button>
  `;
}

function renderProjectAssets(p) {
  return `
    <div class="asset-sections">
      <section>
        <h3>需求分析</h3>
        <div class="list">${(p.requirementAnalyses || []).map((x) => `<div class="list-item"><pre>${escapeHtml(JSON.stringify(x.content_json, null, 2))}</pre></div>`).join("") || `<p class="muted">尚未运行需求分析。</p>`}</div>
      </section>
      <section>
        <h3>产品匹配</h3>
        <div class="list">${(p.productMatches || []).map((x) => `<div class="list-item"><b>${escapeHtml(x.product_name)} / ${escapeHtml(x.module_name || "")}</b><div class="muted">${escapeHtml(x.capability)}</div></div>`).join("") || `<p class="muted">尚未运行产品匹配。</p>`}</div>
      </section>
      <section>
        <h3>案例推荐</h3>
        <div class="list">${(p.caseMatches || []).map((x) => `<div class="list-item"><b>${escapeHtml(x.case_name)}</b><div class="muted">${escapeHtml(x.similarity_reason || "")}</div></div>`).join("") || `<p class="muted">尚未运行案例推荐。</p>`}</div>
      </section>
      <section>
        <h3>架构设计</h3>
        <div class="list">${(p.architectures || []).map((x) => `<div class="list-item"><b>${escapeHtml(x.content_json?.summary || "技术架构")}</b><pre>${escapeHtml(x.mermaid_text || "")}</pre></div>`).join("") || `<p class="muted">尚未运行架构设计。</p>`}</div>
      </section>
      <section>
        <h3>方案章节</h3>
        <div class="list">${(p.proposalSections || []).map((x) => `<div class="list-item"><b>${escapeHtml(x.title)}</b><div class="muted">${escapeHtml(x.content_markdown || "")}</div></div>`).join("") || `<p class="muted">尚未生成章节。</p>`}</div>
      </section>
      <section>
        <h3>PPT 页面</h3>
        <div class="list">${(p.pptPages || []).map((x) => `<div class="list-item"><b>${escapeHtml(x.title)}</b><div class="muted">${escapeHtml(x.page_type)}</div></div>`).join("") || `<p class="muted">尚未生成 PPT 页面。</p>`}</div>
      </section>
      <section class="span-12 asset-run-log">
        <h3>任务运行结果</h3>
        <div class="list">${(p.agentRuns || []).map(renderAgentRun).join("") || `<p class="muted">尚未运行任务。</p>`}</div>
      </section>
    </div>
  `;
}

function renderAgentRun(run) {
  const output = parseMaybeJson(run.output_json);
  return `
    <div class="list-item run-output">
      <div class="run-output-head">
        <b>${escapeHtml(run.skill_name || run.skill_code)}</b>
        <span class="chip ${run.status === "COMPLETED" ? "ok" : "warn"}">${escapeHtml(run.status)}</span>
      </div>
      <div class="muted">${escapeHtml(run.output_type || "GENERIC")} · ${formatDate(run.finished_at || run.started_at)}</div>
      ${output.answer ? `<div class="run-answer">${escapeHtml(output.answer)}</div>` : ""}
      ${run.error_message ? `<div class="muted">错误：${escapeHtml(run.error_message)}</div>` : ""}
    </div>
  `;
}

function renderProjectForm(mode, project = null) {
  const isEdit = mode === "edit";
  const selectedScopes = new Set((project?.knowledgeScopes || []).map((x) => String(x.id)));
  const selectedDeliverables = new Set((project?.deliverables || []).map((x) => x.deliverable_type));
  return `
    ${header(
      isEdit ? "编辑项目" : "新建方案项目",
      "录入客户背景、原始需求、模型配置和 ima 知识库范围。",
      isEdit ? `<button class="btn ghost" onclick="openProject('${project.id}')">取消编辑</button>` : `<button class="btn ghost" onclick="setView('project-list')">返回列表</button>`
    )}
    <form class="panel form" onsubmit="${isEdit ? `submitProjectUpdate(event, '${project.id}')` : "submitProjectCreate(event)"}">
      ${field("项目名称", "projectName", "input", "如：自然资源一张图智能化升级方案", "wide", true, project?.name)}
      ${field("客户名称", "customerName", "input", "客户单位名称", "", true, project?.customer_name)}
      ${selectField("行业", "industry", ["自然资源", "智慧园区", "水利", "应急", "交通", "城市运行", "数字孪生"], project?.industry)}
      ${selectField("客户类型", "customerType", ["政府", "企业", "园区", "集团", "事业单位"], project?.customer_type)}
      ${selectField("默认模型", "modelProfileId", state.modelProfiles.map((m) => [m.id, `${m.name} · ${m.model_name}`]), project?.model_profile_id)}
      ${field("项目背景", "background", "textarea", "项目背景、政策依据、现状问题", "wide", false, project?.background)}
      ${field("原始需求", "rawDemand", "textarea", "粘贴客户需求、会议纪要或招标片段", "wide", true, project?.raw_demand)}
      ${field("已有系统", "existingSystems", "textarea", "已有平台、业务系统、数据资源", "wide", false, project?.existing_systems)}
      ${field("预算", "budget", "input", "暂未明确", "", false, project?.budget)}
      ${field("交付时间", "deliveryTime", "input", "8 周初稿", "", false, project?.delivery_time)}
      <div class="field wide">
        <label>ima 知识范围</label>
        <div class="checkboxes">
          ${state.knowledgeScopes.filter((x) => x.enabled).map((x) => `<label class="check-pill"><input type="checkbox" name="knowledgeScopeIds" value="${x.id}" ${!isEdit || selectedScopes.has(String(x.id)) ? "checked" : ""} />${escapeHtml(x.name)}</label>`).join("")}
        </div>
      </div>
      <div class="field wide">
        <label>交付物</label>
        <div class="checkboxes">
          ${["WORD", "PPT", "ARCHITECTURE", "PRODUCT_LIST", "IMPLEMENTATION_PLAN"].map((x) => `<label class="check-pill"><input type="checkbox" name="deliverables" value="${x}" ${!isEdit || selectedDeliverables.has(x) ? "checked" : ""} />${x}</label>`).join("")}
        </div>
      </div>
      <div class="toolbar wide">
        <button class="btn primary" type="submit">${isEdit ? "保存修改" : "创建并进入工作台"}</button>
        ${isEdit ? `<button class="btn danger" type="button" onclick="deleteProject('${project.id}', '${escapeAttr(project.name)}')">删除项目</button>` : ""}
      </div>
    </form>
  `;
}

function field(label, id, type, placeholder, extra = "", required = false, value = "") {
  const req = required ? "required" : "";
  const safe = escapeAttr(value || "");
  const input = type === "textarea"
    ? `<textarea id="${id}" class="textarea" placeholder="${escapeAttr(placeholder)}" ${req}>${escapeHtml(value || "")}</textarea>`
    : `<input id="${id}" class="input" placeholder="${escapeAttr(placeholder)}" value="${safe}" ${req} />`;
  return `<div class="field ${extra}"><label>${label}</label>${input}</div>`;
}

function selectField(label, id, options, value = "") {
  const normalized = options.map((x) => Array.isArray(x) ? x : [x, x]);
  return `<div class="field"><label>${label}</label><select id="${id}" class="select">${normalized.map(([optionValue, text]) => `<option value="${escapeAttr(optionValue)}" ${String(optionValue) === String(value || "") ? "selected" : ""}>${escapeHtml(text)}</option>`).join("")}</select></div>`;
}

function projectPayload(form) {
  return {
    name: document.getElementById("projectName").value,
    customerName: document.getElementById("customerName").value,
    industry: document.getElementById("industry").value,
    customerType: document.getElementById("customerType").value,
    modelProfileId: document.getElementById("modelProfileId").value || null,
    background: document.getElementById("background").value,
    rawDemand: document.getElementById("rawDemand").value,
    existingSystems: document.getElementById("existingSystems").value,
    budget: document.getElementById("budget").value,
    deliveryTime: document.getElementById("deliveryTime").value,
    knowledgeScopeIds: [...form.querySelectorAll("input[name='knowledgeScopeIds']:checked")].map((x) => x.value),
    deliverables: [...form.querySelectorAll("input[name='deliverables']:checked")].map((x) => x.value),
  };
}

async function submitProjectCreate(event) {
  event.preventDefault();
  const data = await request("/projects", {
    method: "POST",
    body: JSON.stringify(projectPayload(event.target)),
  });
  await loadBase();
  await openProject(data.id);
  toast("项目已创建，已进入工作台");
}

async function submitProjectUpdate(event, id) {
  event.preventDefault();
  const data = await request(`/projects/${id}`, {
    method: "PUT",
    body: JSON.stringify(projectPayload(event.target)),
  });
  state.project = data;
  state.projectMode = "view";
  await loadBase();
  renderApp();
  toast("项目已更新");
}

async function deleteProject(id, name) {
  if (!confirm(`确认删除项目「${name}」？相关 Agent 运行和方案资产会一起删除。`)) return;
  await request(`/projects/${id}`, { method: "DELETE" });
  state.project = null;
  state.projectMode = "view";
  await setView("project-list");
  toast("项目已删除");
}

async function runAgent(projectId, skill) {
  await request(`/projects/${projectId}/agents/${skill}/run`, {
    method: "POST",
    body: JSON.stringify({ modelProfileId: document.getElementById("workspaceModelProfileId")?.value || null }),
  });
  await openProject(projectId);
  toast(`${stepLabel(skill)} 已运行`);
}

async function runPipeline(projectId) {
  for (const step of workflowTasks()) {
    await request(`/projects/${projectId}/agents/${step.code}/run`, {
      method: "POST",
      body: JSON.stringify({ modelProfileId: document.getElementById("workspaceModelProfileId")?.value || null }),
    });
  }
  await openProject(projectId);
  toast("流程已串联跑通");
}

function stepLabel(code) {
  return (workflowTasks().find((x) => x.code === code) || state.skills.find((x) => x.code === code) || {}).name || code;
}

function renderUsers() {
  return `
    ${header("用户权限", "管理员可新增用户并调整管理员 / 牛马专用角色。", `<button class="btn primary" onclick="createUser()">新增牛马专用</button>`)}
    <section class="grid">
      <div class="panel span-8">
        <div class="table-wrap"><table><thead><tr><th>姓名</th><th>邮箱</th><th>角色</th><th>状态</th><th>调整角色</th></tr></thead><tbody>
        ${state.users.map((u) => `<tr><td>${escapeHtml(u.name)}</td><td>${escapeHtml(u.email)}</td><td>${String(u.roles || "").split(",").map(roleLabel).join("，")}</td><td>${u.status}</td><td><select class="select" onchange="updateUserRole('${u.id}', this.value)">${state.roles.map((r) => `<option value="${r.code}" ${String(u.roles).includes(r.code) ? "selected" : ""}>${r.name}</option>`).join("")}</select></td></tr>`).join("")}
        </tbody></table></div>
      </div>
      <form class="panel form span-4" onsubmit="submitUser(event)">
        <div class="panel-title wide"><div><h3>新增用户</h3><p>默认可设为牛马专用</p></div></div>
        ${field("姓名", "newUserName", "input", "姓名", "wide", true)}
        ${field("邮箱", "newUserEmail", "input", "邮箱", "wide", true)}
        ${selectField("角色", "newUserRole", state.roles.map((r) => [r.code, r.name]))}
        <button class="btn primary wide" type="submit">创建用户</button>
      </form>
    </section>
  `;
}

async function createUser() {
  const suffix = Date.now();
  await request("/users", {
    method: "POST",
    body: JSON.stringify({ name: "牛马专用", email: `worker${suffix}@example.com`, roleCode: "WORKER" }),
  });
  await setView("users");
  toast("用户已创建");
}

async function submitUser(event) {
  event.preventDefault();
  await request("/users", {
    method: "POST",
    body: JSON.stringify({
      name: document.getElementById("newUserName").value,
      email: document.getElementById("newUserEmail").value,
      roleCode: document.getElementById("newUserRole").value,
    }),
  });
  await setView("users");
  toast("用户已创建");
}

async function updateUserRole(id, roleCode) {
  await request(`/users/${id}/role`, { method: "PUT", body: JSON.stringify({ roleCode }) });
  await setView("users");
  toast("角色已更新");
}

function renderModels() {
  const editing = state.modelProfiles.find((m) => String(m.id) === String(state.editingModelId));
  return `
    ${header("大模型配置", "配置 OpenAI-compatible 模型，后续供 Agent 调用。")}
    <section class="grid">
      <div class="panel span-8">
        <div class="panel-title"><div><h3>模型列表</h3><p>测试会真实请求 API Base 的 /chat/completions</p></div></div>
        <div class="field">
          <label>业务场景测试 Prompt</label>
          <textarea id="modelTestPrompt" class="textarea">你是 GIS 解决方案助手。请用三句话给出“自然资源一张图平台”的方案价值、核心能力和交付注意事项。</textarea>
        </div>
        <div class="table-wrap model-table"><table><thead><tr><th>名称</th><th>供应商</th><th>模型</th><th>API Base</th><th>用途</th><th>Key</th><th>状态</th><th>操作</th></tr></thead><tbody>
        ${state.modelProfiles.map((m) => `<tr><td>${escapeHtml(m.name)}</td><td>${escapeHtml(m.provider_name)}</td><td>${escapeHtml(m.model_name)}</td><td>${escapeHtml(m.api_base)}</td><td>${escapeHtml(m.use_for || "")}</td><td>${m.api_key_configured ? '<span class="chip ok">已配置</span>' : '<span class="chip warn">未配置</span>'}</td><td><span class="chip ${m.status === "READY" ? "ok" : m.status === "ERROR" ? "danger-chip" : "warn"}">${m.status}</span></td><td><div class="toolbar"><button class="btn small" onclick="testModel('${m.id}')">测试</button><button class="btn small ghost" onclick="editModel('${m.id}')">编辑</button><button class="btn small danger" onclick="deleteModel('${m.id}', '${escapeAttr(m.name)}')">删除</button></div></td></tr>`).join("")}
        </tbody></table></div>
        ${state.modelTestResult ? renderJsonResult("模型测试结果", state.modelTestResult) : ""}
      </div>
      <form class="panel form span-4" onsubmit="submitModel(event)">
        <div class="panel-title wide"><div><h3>${editing ? "编辑模型" : "新增模型"}</h3><p>API Key 后端会加密占位保存，留空则沿用旧 Key</p></div></div>
        ${selectField("供应商", "modelProvider", state.modelProviders.map((p) => [p.code, p.name]), editing?.provider_code)}
        ${field("配置名称", "modelName", "input", "如：默认方案模型", "wide", true, editing?.name)}
        ${field("模型名", "modelModelName", "input", "如：deepseek-chat / qwen-max", "wide", true, editing?.model_name)}
        ${field("API Base", "modelApiBase", "input", "https://api.deepseek.com", "wide", true, editing?.api_base)}
        ${field("API Key", "modelApiKey", "input", "可先留空", "wide")}
        ${field("用途", "modelUseFor", "input", "复杂推理、方案正文、质检", "wide", false, editing?.use_for)}
        <button class="btn primary wide" type="submit">${editing ? "保存修改" : "保存模型"}</button>
        ${editing ? `<button class="btn ghost wide" type="button" onclick="cancelModelEdit()">取消编辑</button>` : ""}
      </form>
    </section>
  `;
}

async function submitModel(event) {
  event.preventDefault();
  const payload = {
      providerCode: document.getElementById("modelProvider").value,
      name: document.getElementById("modelName").value,
      modelName: document.getElementById("modelModelName").value,
      apiBase: document.getElementById("modelApiBase").value,
      apiKey: document.getElementById("modelApiKey").value,
      useFor: document.getElementById("modelUseFor").value,
      status: "PENDING",
    };
  await request(state.editingModelId ? `/model-profiles/${state.editingModelId}` : "/model-profiles", {
    method: state.editingModelId ? "PUT" : "POST",
    body: JSON.stringify(payload),
  });
  state.editingModelId = null;
  await setView("models");
  toast("模型配置已保存");
}

function editModel(id) {
  state.editingModelId = id;
  renderApp();
}

function cancelModelEdit() {
  state.editingModelId = null;
  renderApp();
}

async function deleteModel(id, name) {
  if (!confirm(`确认删除模型配置「${name}」？`)) return;
  await request(`/model-profiles/${id}`, { method: "DELETE" });
  state.editingModelId = null;
  await setView("models");
  toast("模型配置已删除");
}

async function testModel(id) {
  const prompt = document.getElementById("modelTestPrompt")?.value || "";
  try {
    state.modelTestResult = await request(`/model-profiles/${id}/test`, {
      method: "POST",
      body: JSON.stringify({ prompt }),
    });
    await loadBase();
    renderApp();
    toast(state.modelTestResult.success ? "模型测试成功" : "模型返回错误");
  } catch (error) {
    state.modelTestResult = { success: false, error: error.message };
    renderApp();
    toast("模型测试失败");
  }
}

function renderIma() {
  return `
    ${header("ima Skill", "绑定 ima Skill API Key，生成 skill 调用指令并检测 endpoint。")}
    <section class="grid">
      <form class="panel form span-5" onsubmit="submitIma(event)">
        <div class="panel-title wide"><div><h3>绑定 ima Skill</h3><p>${state.ima?.endpoint || ""}</p></div></div>
        ${field("绑定账号", "imaAccount", "input", "ima 账号 / 备注", "wide")}
        ${field("Client ID", "imaClientId", "input", "从 ima agent-interface 获取", "wide", true)}
        ${field("API Key", "imaKey", "input", "从 ima agent-interface 获取", "wide", true)}
        <button class="btn primary wide" type="submit">保存绑定</button>
        <div class="field wide">
          <label>检索测试问题</label>
          <textarea id="imaQuery" class="textarea">帮我查询 SuperMap iPortal 在资源注册、检索、授权和共享方面的能力。</textarea>
        </div>
        <button class="btn wide" type="button" onclick="testIma()">测试 ima 调用</button>
        <button class="btn wide" type="button" onclick="syncImaSubscriptions()">同步订阅库</button>
      </form>
      <div class="panel span-7">
        <div class="panel-title"><div><h3>状态与能力</h3><p>按 ima Skill API Key 方式接入</p></div></div>
        <div class="list">
          <div class="list-item"><b>状态</b><div class="muted">${state.ima?.bound ? "已绑定" : "未绑定"}</div></div>
          <div class="list-item"><b>绑定账号</b><div class="muted">${escapeHtml(state.ima?.binding?.bound_account || "-")}</div></div>
          <div class="list-item"><b>Client ID</b><div class="muted">${escapeHtml(state.ima?.binding?.clientIdMasked || "未配置")}</div></div>
          <div class="list-item"><b>API Key</b><div class="muted">${escapeHtml(state.ima?.binding?.apiKeyMasked || "未配置")}</div></div>
          <div class="list-item"><b>接入方式</b><div class="muted">${escapeHtml(state.ima?.integrationMode || "")}</div></div>
          <div class="list-item"><b>远端订阅库读取</b><div class="muted">${escapeHtml(state.ima?.remoteSubscriptionStatus?.message || "-")}</div></div>
          <div class="list-item"><b>最近同步</b><div class="muted">${formatDate(state.ima?.syncSummary?.lastSyncedAt)} · ${state.ima?.syncSummary?.count || 0} 个库</div></div>
          <div class="list-item"><b>禁用来源</b><div class="muted">${state.ima?.disabledSources || ""}</div></div>
          ${(state.ima?.capabilities || []).map((x) => `<div class="list-item">${x}</div>`).join("")}
        </div>
        <div class="panel-title subscriptions-title"><div><h3>ima 订阅库列表</h3><p>${escapeHtml(state.ima?.subscriptionSource || "ima OpenAPI 同步结果")}</p></div><button class="btn ghost" onclick="syncImaSubscriptions()">同步</button></div>
        <div class="list">
          ${(state.ima?.subscriptions || []).map(renderImaSubscription).join("") || `<div class="list-item muted">暂无订阅库，保存 Client ID / API Key 后点击同步。</div>`}
        </div>
        ${state.imaTestResult ? renderJsonResult("ima 测试结果", state.imaTestResult) : ""}
      </div>
    </section>
  `;
}

function renderImaSubscription(s) {
  const meta = [
    s.base_type,
    s.role_type,
    s.content_count ? `${s.content_count} 条内容` : "",
    s.member_count ? `${s.member_count} 成员` : "",
    s.can_add ? "可添加" : "可检索",
  ].filter(Boolean).join(" · ");
  return `
    <div class="list-item">
      <b>${escapeHtml(s.name)}</b>
      <div class="muted">${escapeHtml(meta || s.status || "")}</div>
      <div class="muted tiny">ID: ${escapeHtml(s.id || s.external_id || "")}</div>
    </div>
  `;
}

function renderSkills() {
  const editing = state.skills.find((s) => String(s.id) === String(state.editingSkillId)) || state.skills[0];
  const toolPolicyText = typeof editing?.tool_policy_json === "string" ? editing.tool_policy_json : JSON.stringify(editing?.tool_policy_json || {}, null, 2);
  return `
    ${header("Agent Skill 管理", "新增、配置、排序和停用工作台可执行任务。")}
    <section class="grid">
      <div class="panel span-5">
        <div class="panel-title"><div><h3>任务列表</h3><p>${state.skills.length} 个 Skill / 任务</p></div></div>
        <div class="list">
          ${state.skills.map((s) => `<button class="list-item skill-item ${String(s.id) === String(editing?.id) ? "active-skill" : ""}" onclick="editSkill('${s.id}')"><b>${escapeHtml(s.name)}</b><span class="muted">${escapeHtml(s.code)} · ${escapeHtml(s.category || "WORKFLOW")} · ${escapeHtml(s.output_type || "GENERIC")} · ${s.enabled ? "启用" : "停用"}</span></button>`).join("")}
        </div>
      </div>
      <form class="panel form span-7" onsubmit="submitSkill(event, '${editing?.id || ""}')">
        <div class="panel-title wide"><div><h3>${escapeHtml(editing?.name || "选择 Skill")}</h3><p>${escapeHtml(editing?.code || "")}</p></div></div>
        ${field("名称", "skillName", "input", "Skill 名称", "wide", true, editing?.name)}
        ${selectField("任务分类", "skillCategory", [["WORKFLOW", "工作台任务"], ["TOOL", "工具 Skill"]], editing?.category || "WORKFLOW")}
        ${selectField("输出类型", "skillOutputType", [["GENERIC", "通用产出"], ["REQUIREMENT", "需求分析"], ["PRODUCT", "产品匹配"], ["CASE", "案例推荐"], ["ARCHITECTURE", "架构设计"], ["PROPOSAL", "方案章节"], ["PPT", "PPT 页面"], ["QA", "方案质检"], ["KNOWLEDGE", "知识工具"]], editing?.output_type || "GENERIC")}
        ${field("排序", "skillSortOrder", "input", "10", "", false, editing?.sort_order ?? 100)}
        ${field("说明", "skillDescription", "textarea", "Skill 说明", "wide", false, editing?.description)}
        ${field("Prompt 模板", "skillPrompt", "textarea", "工作台执行时会拼接项目上下文", "wide", true, editing?.prompt_template)}
        ${field("工具策略 JSON", "skillToolPolicy", "textarea", "{\"knowledge\":true}", "wide", false, toolPolicyText)}
        <label class="check-pill wide"><input id="skillEnabled" type="checkbox" ${editing?.enabled ? "checked" : ""} />启用该 Skill</label>
        <button class="btn primary wide" type="submit">保存 Skill 配置</button>
        <button class="btn danger wide" type="button" onclick="disableSkill('${editing?.id || ""}', '${escapeAttr(editing?.name || "")}')">停用该任务</button>
      </form>
      <form class="panel form span-12 task-create-panel" onsubmit="submitSkillCreate(event)">
        <div class="panel-title wide"><div><h3>新增可执行任务</h3><p>新增后会按排序出现在项目工作台，可选择不同输出类型进入对应资产区。</p></div></div>
        ${field("任务编码", "newSkillCode", "input", "如：competitor-analysis", "", true)}
        ${field("任务名称", "newSkillName", "input", "如：竞品分析", "", true)}
        ${selectField("任务分类", "newSkillCategory", [["WORKFLOW", "工作台任务"], ["TOOL", "工具 Skill"]], "WORKFLOW")}
        ${selectField("输出类型", "newSkillOutputType", [["GENERIC", "通用产出"], ["REQUIREMENT", "需求分析"], ["PRODUCT", "产品匹配"], ["CASE", "案例推荐"], ["ARCHITECTURE", "架构设计"], ["PROPOSAL", "方案章节"], ["PPT", "PPT 页面"], ["QA", "方案质检"], ["KNOWLEDGE", "知识工具"]], "GENERIC")}
        ${field("排序", "newSkillSortOrder", "input", "80", "", false, 80)}
        ${field("说明", "newSkillDescription", "textarea", "这个任务解决什么问题", "wide", false)}
        ${field("Prompt 模板", "newSkillPrompt", "textarea", "请基于项目上下文完成任务，输出结构化、可进入方案的内容。", "wide", true)}
        ${field("工具策略 JSON", "newSkillToolPolicy", "textarea", "{\"knowledge\":true,\"citationRequired\":true}", "wide", false, "{\"knowledge\":true,\"citationRequired\":true}")}
        <button class="btn primary wide" type="submit">新增任务</button>
      </form>
    </section>
  `;
}

function editSkill(id) {
  state.editingSkillId = id;
  renderApp();
}

async function submitSkill(event, id) {
  event.preventDefault();
  await request(`/skills/${id}`, {
    method: "PUT",
    body: JSON.stringify(skillPayload("skill")),
  });
  await setView("skills");
  toast("Skill 配置已保存");
}

async function submitSkillCreate(event) {
  event.preventDefault();
  await request("/skills", {
    method: "POST",
    body: JSON.stringify(skillPayload("newSkill")),
  });
  await setView("skills");
  toast("任务已新增");
}

async function disableSkill(id, name) {
  if (!id || !confirm(`确认停用任务「${name}」？历史运行结果会保留。`)) return;
  await request(`/skills/${id}`, { method: "DELETE" });
  await setView("skills");
  toast("任务已停用");
}

function skillPayload(prefix) {
  return {
    code: document.getElementById(`${prefix}Code`)?.value,
    name: document.getElementById(`${prefix}Name`).value,
    description: document.getElementById(`${prefix}Description`).value,
    category: document.getElementById(`${prefix}Category`).value,
    outputType: document.getElementById(`${prefix}OutputType`).value,
    sortOrder: Number(document.getElementById(`${prefix}SortOrder`).value || 100),
    promptTemplate: document.getElementById(`${prefix}Prompt`).value,
    toolPolicyJson: document.getElementById(`${prefix}ToolPolicy`).value,
    enabled: prefix === "skill" ? document.getElementById("skillEnabled").checked : true,
  };
}

async function submitIma(event) {
  event.preventDefault();
  await request("/ima-skill/bind", {
    method: "POST",
    body: JSON.stringify({
      apiKey: document.getElementById("imaKey").value,
      clientId: document.getElementById("imaClientId").value,
      boundAccount: document.getElementById("imaAccount").value,
    }),
  });
  await setView("ima");
  toast("ima Skill 已绑定");
}

async function syncImaSubscriptions() {
  const data = await request("/ima-skill/sync-subscriptions", { method: "POST" });
  await loadBase();
  state.imaTestResult = { summary: `已同步 ${data.syncedCount || 0} 个 ima 订阅库`, syncedCount: data.syncedCount };
  renderApp();
  toast(`已同步 ${data.syncedCount || 0} 个 ima 订阅库`);
}

async function testIma() {
  const data = await request("/ima-skill/test-search", {
    method: "POST",
    body: JSON.stringify({ query: document.getElementById("imaQuery")?.value || "iPortal 资源授权能力" }),
  });
  state.imaTestResult = data;
  renderApp();
  toast(data.summary);
}

function renderJsonResult(title, data) {
  return `
    <div class="result-box">
      <h3>${escapeHtml(title)}</h3>
      ${data.answer ? `<div class="list-item"><b>模型回答</b><div class="muted">${escapeHtml(data.answer)}</div></div>` : ""}
      ${data.instruction ? `<div class="list-item"><b>ima 调用指令</b><div class="muted">${escapeHtml(data.instruction)}</div></div>` : ""}
      <pre>${escapeHtml(JSON.stringify(data, null, 2))}</pre>
    </div>
  `;
}

function renderKnowledge() {
  return `
    ${header("知识范围", "只管理 ima 知识库范围，不接企业微信和本机文档。")}
    <section class="grid">
      <div class="panel span-8">
        <div class="table-wrap"><table><thead><tr><th>名称</th><th>类型</th><th>来源</th><th>归属</th><th>检索边界</th><th>状态</th></tr></thead><tbody>
        ${state.knowledgeScopes.map((k) => `<tr><td>${escapeHtml(k.name)}${k.external_id ? `<div class="muted tiny">${escapeHtml(k.external_id)}</div>` : ""}</td><td>${escapeHtml(k.type)}</td><td>${escapeHtml(k.source || "MANUAL")}</td><td>${escapeHtml(k.owner)}</td><td>${escapeHtml(k.scope_prompt)}</td><td>${k.enabled ? '<span class="chip ok">启用</span>' : '<span class="chip">停用</span>'}</td></tr>`).join("")}
        </tbody></table></div>
      </div>
      <form class="panel form span-4" onsubmit="submitKnowledge(event)">
        <div class="panel-title wide"><div><h3>新增知识范围</h3><p>用于项目选择和 Agent 检索</p></div></div>
        ${field("名称", "scopeName", "input", "ima 产品资料库", "wide", true)}
        ${field("类型", "scopeType", "input", "官方产品 / 技术资料 / 案例", "wide", true)}
        ${field("检索边界", "scopePrompt", "textarea", "描述这个知识范围适合检索什么", "wide", true)}
        <button class="btn primary wide" type="submit">保存知识范围</button>
      </form>
    </section>
  `;
}

async function submitKnowledge(event) {
  event.preventDefault();
  await request("/knowledge-scopes", {
    method: "POST",
    body: JSON.stringify({
      name: document.getElementById("scopeName").value,
      type: document.getElementById("scopeType").value,
      owner: "ima 账号",
      scopePrompt: document.getElementById("scopePrompt").value,
      enabled: true,
    }),
  });
  await setView("knowledge");
  toast("知识范围已保存");
}

function selectedProject() {
  if (!state.project) throw new Error("请先打开一个项目工作台");
  return state.project;
}

function buildMarkdown(p) {
  const requirement = (p.requirementAnalyses || [])
    .map((x) => `\n\`\`\`json\n${JSON.stringify(x.content_json || {}, null, 2)}\n\`\`\``)
    .join("\n");
  const products = (p.productMatches || [])
    .map((x) => `- ${x.product_name || "产品能力"}${x.module_name ? ` / ${x.module_name}` : ""}：${x.capability || ""}`)
    .join("\n");
  const cases = (p.caseMatches || [])
    .map((x) => `- ${x.case_name || "案例"}：${x.similarity_reason || ""}`)
    .join("\n");
  const architectures = (p.architectures || [])
    .map((x) => `### ${x.content_json?.summary || "技术架构"}\n\n\`\`\`mermaid\n${x.mermaid_text || ""}\n\`\`\``)
    .join("\n\n");
  const sections = (p.proposalSections || [])
    .map((x) => `## ${x.title}\n${x.content_markdown || ""}`)
    .join("\n\n");

  return `# ${p.name}

客户：${p.customer_name || "-"}
行业：${p.industry || "-"}
客户类型：${p.customer_type || "-"}
交付时间：${p.delivery_time || "-"}

## 项目背景
${p.background || "-"}

## 原始需求
${p.raw_demand || "-"}

## 已有系统
${p.existing_systems || "-"}

## 需求分析
${requirement || "尚未生成"}

## 产品匹配
${products || "尚未生成"}

## 案例推荐
${cases || "尚未生成"}

## 技术架构
${architectures || "尚未生成"}

${sections || "## 方案正文\n尚未生成"}
`;
}

function markdownToWordHtml(markdown, title) {
  const body = markdown.split("\n").map((line) => {
    if (line.startsWith("# ")) return `<h1>${escapeHtml(line.slice(2))}</h1>`;
    if (line.startsWith("## ")) return `<h2>${escapeHtml(line.slice(3))}</h2>`;
    if (line.startsWith("### ")) return `<h3>${escapeHtml(line.slice(4))}</h3>`;
    if (line.startsWith("- ")) return `<p>• ${escapeHtml(line.slice(2))}</p>`;
    if (line.startsWith("```")) return "";
    return line.trim() ? `<p>${escapeHtml(line)}</p>` : "<p></p>";
  }).join("");
  return `<html><head><meta charset="utf-8"><title>${escapeHtml(title)}</title><style>body{font-family:Microsoft YaHei,Arial,sans-serif;line-height:1.7;color:#111827}h1,h2,h3{color:#0f172a}p{margin:6px 0}</style></head><body>${body}</body></html>`;
}

function downloadMarkdown() {
  const p = selectedProject();
  download(`${safeFilename(p.name)}.md`, buildMarkdown(p), "text/markdown;charset=utf-8");
}

function downloadWord() {
  const p = selectedProject();
  download(`${safeFilename(p.name)}.doc`, markdownToWordHtml(buildMarkdown(p), p.name), "application/msword;charset=utf-8");
}

function downloadPptMarkdown() {
  const p = selectedProject();
  const text = (p.pptPages || []).map((x) => `## ${x.sort_order || ""}. ${x.title || "PPT 页面"}\n- 页面类型：${x.page_type || "-"}\n- 页面要点：${JSON.stringify(x.content_json || {}, null, 2)}`).join("\n\n");
  download(`${safeFilename(p.name)}-PPT大纲.md`, text || "尚未生成 PPT 页面", "text/markdown;charset=utf-8");
}

function downloadMermaid() {
  const p = selectedProject();
  const text = (p.architectures || []).map((x) => x.mermaid_text || "").filter(Boolean).join("\n\n");
  download(`${safeFilename(p.name)}-architecture.mmd`, text || "graph TD\n  A[尚未生成架构图]", "text/plain;charset=utf-8");
}

function downloadJson() {
  const p = selectedProject();
  download(`${safeFilename(p.name)}-context.json`, JSON.stringify(p, null, 2), "application/json;charset=utf-8");
}

function download(filename, content, type) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function safeFilename(name) {
  return String(name || "solution-pilot").replace(/[\\/:*?"<>|]/g, "_");
}

function kv(label, value) {
  return `<div class="kv-row"><b>${label}</b><span>${escapeHtml(value || "-")}</span></div>`;
}

function formatDate(value) {
  if (!value) return "-";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}

function toast(message) {
  const el = document.getElementById("toast");
  if (!el) return;
  el.textContent = message;
  el.classList.add("show");
  setTimeout(() => el.classList.remove("show"), 1800);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function escapeAttr(value) {
  return escapeHtml(value).replaceAll("`", "&#096;");
}

boot().catch((error) => {
  console.error(error);
  localStorage.removeItem("sp.v1.user");
  localStorage.removeItem("sp.v1.token");
  state.user = null;
  renderLogin();
  toast(error.message);
});
