const api = (path) => `${location.origin}/api${path}`;

const state = {
  user: JSON.parse(localStorage.getItem("sp.v1.user") || "null"),
  token: localStorage.getItem("sp.v1.token") || "",
  view: "dashboard",
  users: [],
  roles: [],
  projects: [],
  project: null,
  modelProviders: [],
  modelProfiles: [],
  ima: null,
  knowledgeScopes: [],
  skills: [],
};

const agentLabels = {
  requirement: "需求分析",
  product: "产品匹配",
  case: "案例推荐",
  architecture: "架构设计",
  proposal: "方案章节",
  ppt: "PPT 页面",
  qa: "方案质检",
};

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

function roleLabel(code) {
  if (code === "ADMIN") return "管理员";
  if (code === "WORKER") return "牛马专用";
  return code || "-";
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
          <div><h1>SolutionPilot V1</h1><span>GIS 方案工程平台</span></div>
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
          <button class="btn primary wide" type="submit">进入 V1 工作台</button>
        </form>
      </section>
      <section class="login-side">
        <span class="chip">V1 本地版</span>
        <h2>以 ima 知识库为底座，生成可追踪的方案资产。</h2>
        <p>管理员维护用户、模型和 ima 配置；牛马专用负责创建项目、运行 Agent、查看交付资产。</p>
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
  state.view = "dashboard";
  localStorage.setItem("sp.v1.user", JSON.stringify(data));
  localStorage.setItem("sp.v1.token", data.token);
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
          <div><h1>SolutionPilot</h1><span>V1 本地工作台</span></div>
        </div>
        <div class="user-box">
          <b>${escapeHtml(state.user.name)}</b>
          <span>${(state.user.roles || []).map(roleLabel).join("，")}</span>
        </div>
        <nav class="nav">
          ${nav("dashboard", "总览")}
          ${nav("project-list", "项目列表")}
          ${nav("project-manage", "项目管理")}
          ${nav("project-new", "新建项目")}
          ${isAdmin() ? nav("users", "用户管理") : ""}
          ${isAdmin() ? nav("models", "大模型配置") : ""}
          ${isAdmin() ? nav("ima", "ima 配置") : ""}
          ${isAdmin() ? nav("knowledge", "知识范围") : ""}
        </nav>
        <button class="btn ghost" onclick="logout()">退出</button>
      </aside>
      <main class="content">${renderView()}</main>
    </div>
    <div id="toast" class="toast"></div>
  `;
}

function nav(view, label) {
  return `<button class="${state.view === view ? "active" : ""}" onclick="setView('${view}')">${label}</button>`;
}

async function setView(view) {
  state.view = view;
  if (view !== "project-manage") state.project = null;
  await loadBase();
  renderApp();
}

function renderView() {
  if (state.view === "project-list") return renderProjectList();
  if (state.view === "project-manage") return renderProjectManage();
  if (state.view === "project-new") return renderNewProject();
  if (state.view === "users") return isAdmin() ? renderUsers() : noAccess();
  if (state.view === "models") return isAdmin() ? renderModels() : noAccess();
  if (state.view === "ima") return isAdmin() ? renderIma() : noAccess();
  if (state.view === "knowledge") return isAdmin() ? renderKnowledge() : noAccess();
  return renderDashboard();
}

function header(title, desc, actions = "") {
  return `<div class="topbar"><div><h2>${title}</h2><p>${desc}</p></div><div class="toolbar">${actions}</div></div>`;
}

function noAccess() {
  return `${header("无权限", "当前角色不能访问该页面。")}<section class="panel"><p class="muted">请使用管理员账号登录。</p></section>`;
}

function metric(label, value) {
  return `<article class="card metric span-3"><span>${label}</span><strong>${value}</strong></article>`;
}

function renderDashboard() {
  return `
    ${header("总览", "V1 本地版已接 PostgreSQL，支持项目创建、管理和 mock Agent 落表。", `<button class="btn primary" onclick="setView('project-new')">新建项目</button>`)}
    <section class="grid">
      ${metric("项目数", state.projects.length)}
      ${metric("Agent Skills", state.skills.length)}
      ${metric("模型配置", state.modelProfiles.length)}
      ${metric("ima 知识范围", state.knowledgeScopes.filter((x) => x.enabled).length)}
      <div class="panel span-7">
        <div class="panel-title"><div><h3>最近项目</h3><p>点击项目进入管理页</p></div></div>
        ${projectTable(state.projects.slice(0, 6))}
      </div>
      <div class="panel span-5">
        <div class="panel-title"><div><h3>页面控制</h3><p>按角色展示功能入口</p></div></div>
        <div class="list">
          <div class="list-item"><b>管理员</b><div class="muted">用户管理、模型配置、ima 配置、知识范围、项目全功能</div></div>
          <div class="list-item"><b>牛马专用</b><div class="muted">项目列表、新建项目、项目管理、运行 Agent</div></div>
        </div>
      </div>
    </section>
  `;
}

function renderProjectList() {
  return `
    ${header("项目列表", "查看所有项目，进入项目管理或新建项目。", `<button class="btn primary" onclick="setView('project-new')">新建项目</button>`)}
    <section class="panel">
      <div class="panel-title"><div><h3>全部项目</h3><p>${state.projects.length} 个项目</p></div></div>
      ${projectTable(state.projects)}
    </section>
  `;
}

function projectTable(items) {
  return `<div class="table-wrap"><table><thead><tr><th>项目</th><th>客户</th><th>行业</th><th>状态</th><th>进度</th><th>操作</th></tr></thead><tbody>
    ${items.map((p) => `<tr>
      <td>${escapeHtml(p.name)}</td>
      <td>${escapeHtml(p.customer_name || "")}</td>
      <td>${escapeHtml(p.industry || "")}</td>
      <td><span class="chip">${p.status}</span></td>
      <td>${p.progress || 0}%</td>
      <td><button class="btn small ghost" onclick="openProject('${p.id}')">管理</button></td>
    </tr>`).join("") || `<tr><td colspan="6" class="muted">暂无项目</td></tr>`}
  </tbody></table></div>`;
}

async function openProject(id) {
  state.project = await request(`/projects/${id}`);
  state.view = "project-manage";
  renderApp();
}

function renderProjectManage() {
  if (!state.project) {
    return `
      ${header("项目管理", "选择一个项目进入工作台。", `<button class="btn primary" onclick="setView('project-new')">新建项目</button>`)}
      <section class="panel">${projectTable(state.projects)}</section>
    `;
  }
  const p = state.project;
  return `
    ${header("项目管理", `${escapeHtml(p.customer_name)} · ${escapeHtml(p.industry || "")} · ${escapeHtml(p.status)}`, `<button class="btn ghost" onclick="setView('project-list')">返回列表</button>`)}
    <section class="workspace wide-workspace">
      <div class="panel">
        <div class="panel-title"><div><h3>${escapeHtml(p.name)}</h3><p>运行 Agent 后会写入业务资产表</p></div></div>
        <div class="agent-grid">
          ${Object.entries(agentLabels).map(([key, label]) => `<button class="btn" onclick="runAgent('${p.id}', '${key}')">${label}</button>`).join("")}
        </div>
        <div class="grid asset-grid">
          ${asset("Agent Runs", p.agentRuns?.length || 0)}
          ${asset("需求分析", p.requirementAnalyses?.length || 0)}
          ${asset("产品匹配", p.productMatches?.length || 0)}
          ${asset("案例推荐", p.caseMatches?.length || 0)}
          ${asset("技术架构", p.architectures?.length || 0)}
          ${asset("方案章节", p.proposalSections?.length || 0)}
          ${asset("PPT 页面", p.pptPages?.length || 0)}
        </div>
        ${renderProjectAssets(p)}
      </div>
      <aside class="panel">
        <div class="panel-title"><div><h3>项目上下文</h3><p>创建项目时写入</p></div></div>
        <div class="list">
          <div class="list-item"><b>客户</b><div class="muted">${escapeHtml(p.customer_name)}</div></div>
          <div class="list-item"><b>行业</b><div class="muted">${escapeHtml(p.industry || "-")}</div></div>
          <div class="list-item"><b>知识范围</b><div class="muted">${(p.knowledgeScopes || []).map((x) => x.name).join("，") || "-"}</div></div>
          <div class="list-item"><b>交付物</b><div class="muted">${(p.deliverables || []).map((x) => x.deliverable_type).join("，") || "-"}</div></div>
        </div>
      </aside>
    </section>
  `;
}

function asset(label, value) {
  return `<article class="card metric span-3"><span>${label}</span><strong>${value}</strong></article>`;
}

function renderProjectAssets(p) {
  return `
    <div class="asset-sections">
      <section>
        <h3>产品匹配</h3>
        <div class="list">${(p.productMatches || []).map((x) => `<div class="list-item"><b>${escapeHtml(x.product_name)} / ${escapeHtml(x.module_name || "")}</b><div class="muted">${escapeHtml(x.capability)}</div></div>`).join("") || `<p class="muted">尚未运行产品匹配。</p>`}</div>
      </section>
      <section>
        <h3>方案章节</h3>
        <div class="list">${(p.proposalSections || []).map((x) => `<div class="list-item"><b>${escapeHtml(x.title)}</b><div class="muted">${escapeHtml(x.content_markdown || "")}</div></div>`).join("") || `<p class="muted">尚未生成章节。</p>`}</div>
      </section>
      <section>
        <h3>PPT 页面</h3>
        <div class="list">${(p.pptPages || []).map((x) => `<div class="list-item"><b>${escapeHtml(x.title)}</b><div class="muted">${escapeHtml(x.page_type)}</div></div>`).join("") || `<p class="muted">尚未生成 PPT 页面。</p>`}</div>
      </section>
    </div>
  `;
}

function renderNewProject() {
  return `
    ${header("新建项目", "录入客户需求、选择模型和 ima 知识范围，创建后进入项目管理。")}
    <form class="panel form" onsubmit="submitProject(event)">
      ${field("项目名称", "projectName", "input", "如：自然资源一张图智能化升级方案", "wide", true)}
      ${field("客户名称", "customerName", "input", "客户单位名称", "", true)}
      ${selectField("行业", "industry", ["自然资源", "智慧园区", "水利", "应急", "交通", "城市运行"])}
      ${selectField("客户类型", "customerType", ["政府", "企业", "园区", "集团", "事业单位"])}
      ${selectField("默认模型", "modelProfileId", state.modelProfiles.map((m) => [m.id, `${m.name} · ${m.model_name}`]))}
      ${field("项目背景", "background", "textarea", "项目背景、政策依据、现状问题", "wide")}
      ${field("原始需求", "rawDemand", "textarea", "粘贴客户需求、会议纪要或招标片段", "wide", true)}
      ${field("已有系统", "existingSystems", "textarea", "已有平台、业务系统、数据资源", "wide")}
      ${field("预算", "budget", "input", "暂未明确")}
      ${field("交付时间", "deliveryTime", "input", "8 周初稿")}
      <div class="field wide">
        <label>ima 知识范围</label>
        <div class="checkboxes">${state.knowledgeScopes.filter((x) => x.enabled).map((x) => `<label class="check-pill"><input type="checkbox" name="knowledgeScopeIds" value="${x.id}" checked />${escapeHtml(x.name)}</label>`).join("")}</div>
      </div>
      <div class="field wide">
        <label>交付物</label>
        <div class="checkboxes">${["WORD", "PPT", "ARCHITECTURE", "PRODUCT_LIST"].map((x) => `<label class="check-pill"><input type="checkbox" name="deliverables" value="${x}" checked />${x}</label>`).join("")}</div>
      </div>
      <div class="toolbar wide">
        <button class="btn primary" type="submit">创建项目</button>
        <button class="btn ghost" type="button" onclick="setView('project-list')">取消</button>
      </div>
    </form>
  `;
}

function field(label, id, type, placeholder, extra = "", required = false) {
  const req = required ? "required" : "";
  const input = type === "textarea"
    ? `<textarea id="${id}" class="textarea" placeholder="${placeholder}" ${req}></textarea>`
    : `<input id="${id}" class="input" placeholder="${placeholder}" ${req} />`;
  return `<div class="field ${extra}"><label>${label}</label>${input}</div>`;
}

function selectField(label, id, options) {
  const normalized = options.map((x) => Array.isArray(x) ? x : [x, x]);
  return `<div class="field"><label>${label}</label><select id="${id}" class="select">${normalized.map(([value, text]) => `<option value="${value}">${text}</option>`).join("")}</select></div>`;
}

async function submitProject(event) {
  event.preventDefault();
  const form = event.target;
  const knowledgeScopeIds = [...form.querySelectorAll("input[name='knowledgeScopeIds']:checked")].map((x) => x.value);
  const deliverables = [...form.querySelectorAll("input[name='deliverables']:checked")].map((x) => x.value);
  const data = await request("/projects", {
    method: "POST",
    body: JSON.stringify({
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
      knowledgeScopeIds,
      deliverables,
    }),
  });
  await loadBase();
  await openProject(data.id);
  toast("项目已创建");
}

async function runAgent(projectId, skill) {
  await request(`/projects/${projectId}/agents/${skill}/run`, { method: "POST" });
  await openProject(projectId);
  toast(`${agentLabels[skill] || skill} 已运行`);
}

function renderUsers() {
  return `
    ${header("用户管理", "管理员可新增用户并调整管理员 / 牛马专用角色。", `<button class="btn primary" onclick="createUser()">新增牛马专用</button>`)}
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
  return `
    ${header("大模型配置", "配置 OpenAI-compatible 模型，后续供 Agent 调用。")}
    <section class="grid">
      <div class="panel span-8">
        <div class="table-wrap"><table><thead><tr><th>名称</th><th>供应商</th><th>模型</th><th>API Base</th><th>用途</th><th>状态</th></tr></thead><tbody>
        ${state.modelProfiles.map((m) => `<tr><td>${escapeHtml(m.name)}</td><td>${escapeHtml(m.provider_name)}</td><td>${escapeHtml(m.model_name)}</td><td>${escapeHtml(m.api_base)}</td><td>${escapeHtml(m.use_for || "")}</td><td><span class="chip warn">${m.status}</span></td></tr>`).join("")}
        </tbody></table></div>
      </div>
      <form class="panel form span-4" onsubmit="submitModel(event)">
        <div class="panel-title wide"><div><h3>新增模型</h3><p>API Key 后端会加密占位保存</p></div></div>
        ${selectField("供应商", "modelProvider", state.modelProviders.map((p) => [p.code, p.name]))}
        ${field("配置名称", "modelName", "input", "如：默认方案模型", "wide", true)}
        ${field("模型名", "modelModelName", "input", "如：gpt-4.1 / qwen-max", "wide", true)}
        ${field("API Base", "modelApiBase", "input", "https://api.openai.com/v1", "wide", true)}
        ${field("API Key", "modelApiKey", "input", "可先留空", "wide")}
        ${field("用途", "modelUseFor", "input", "复杂推理、方案正文、质检", "wide")}
        <button class="btn primary wide" type="submit">保存模型</button>
      </form>
    </section>
  `;
}

async function submitModel(event) {
  event.preventDefault();
  await request("/model-profiles", {
    method: "POST",
    body: JSON.stringify({
      providerCode: document.getElementById("modelProvider").value,
      name: document.getElementById("modelName").value,
      modelName: document.getElementById("modelModelName").value,
      apiBase: document.getElementById("modelApiBase").value,
      apiKey: document.getElementById("modelApiKey").value,
      useFor: document.getElementById("modelUseFor").value,
      status: "PENDING",
    }),
  });
  await setView("models");
  toast("模型配置已保存");
}

function renderIma() {
  return `
    ${header("ima 配置", "绑定 ima Skill API Key，测试知识库调用。")}
    <section class="grid">
      <form class="panel form span-5" onsubmit="submitIma(event)">
        <div class="panel-title wide"><div><h3>绑定 ima Skill</h3><p>${state.ima?.endpoint || ""}</p></div></div>
        ${field("绑定账号", "imaAccount", "input", "ima 账号 / 备注", "wide")}
        ${field("API Key", "imaKey", "input", "从 ima agent-interface 获取", "wide", true)}
        <button class="btn primary wide" type="submit">保存绑定</button>
        <button class="btn wide" type="button" onclick="testIma()">测试检索</button>
      </form>
      <div class="panel span-7">
        <div class="panel-title"><div><h3>状态与能力</h3><p>V1 启用检索、读取和引用</p></div></div>
        <div class="list">
          <div class="list-item"><b>状态</b><div class="muted">${state.ima?.bound ? "已绑定" : "未绑定"}</div></div>
          <div class="list-item"><b>禁用来源</b><div class="muted">${state.ima?.disabledSources || ""}</div></div>
          ${(state.ima?.capabilities || []).map((x) => `<div class="list-item">${x}</div>`).join("")}
        </div>
      </div>
    </section>
  `;
}

async function submitIma(event) {
  event.preventDefault();
  await request("/ima-skill/bind", {
    method: "POST",
    body: JSON.stringify({
      apiKey: document.getElementById("imaKey").value,
      boundAccount: document.getElementById("imaAccount").value,
    }),
  });
  await setView("ima");
  toast("ima Skill 已绑定");
}

async function testIma() {
  const data = await request("/ima-skill/test-search", {
    method: "POST",
    body: JSON.stringify({ query: "iPortal 资源授权能力" }),
  });
  toast(data.summary);
}

function renderKnowledge() {
  return `
    ${header("知识范围", "只管理 ima 知识库范围，不接企业微信和本机文档。")}
    <section class="grid">
      <div class="panel span-8">
        <div class="table-wrap"><table><thead><tr><th>名称</th><th>类型</th><th>归属</th><th>检索边界</th><th>状态</th></tr></thead><tbody>
        ${state.knowledgeScopes.map((k) => `<tr><td>${escapeHtml(k.name)}</td><td>${escapeHtml(k.type)}</td><td>${escapeHtml(k.owner)}</td><td>${escapeHtml(k.scope_prompt)}</td><td>${k.enabled ? '<span class="chip ok">启用</span>' : '<span class="chip">停用</span>'}</td></tr>`).join("")}
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

function toast(message) {
  const el = document.getElementById("toast");
  if (!el) return;
  el.textContent = message;
  el.classList.add("show");
  setTimeout(() => el.classList.remove("show"), 1800);
}

function escapeHtml(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

boot().catch((error) => {
  console.error(error);
  localStorage.removeItem("sp.v1.user");
  localStorage.removeItem("sp.v1.token");
  renderLogin();
  toast(error.message);
});
