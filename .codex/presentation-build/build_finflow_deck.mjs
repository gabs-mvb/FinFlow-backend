import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const workspaceDir = "C:\\Users\\gabsm\\Documents\\Projetos\\FinFlow-backend";
const SKILL_DIR = "C:\\Users\\gabsm\\.codex\\plugins\\cache\\openai-primary-runtime\\presentations\\26.915.20218\\skills\\presentations";
const FINAL_PPTX = path.join(workspaceDir, "artifacts", "FinFlow-apresentacao-produto-final.pptx");
const RUNTIME_PYTHON = "C:\\Users\\gabsm\\.cache\\codex-runtimes\\codex-primary-runtime\\dependencies\\python\\python.exe";
const { resolvePresentationFont, finalizePresentation } = await import(
  pathToFileURL(path.join(SKILL_DIR, "container_tools", "artifact_tool_utils.mjs")).href,
);

const font = resolvePresentationFont();
const deck = Presentation.create({ slideSize: { width: 1280, height: 720 } });

const C = {
  navy: "#102A2E",
  navy2: "#173B40",
  teal: "#18A999",
  mint: "#A9E5D5",
  cream: "#F6F3EA",
  white: "#FFFFFF",
  ink: "#183136",
  muted: "#65787A",
  coral: "#EF7A62",
  line: "#C9D8D5",
  pale: "#E7F2EE",
  gold: "#E9B949",
};

function shape(slide, geometry, left, top, width, height, fill = "none", line = "none") {
  return slide.shapes.add({
    geometry,
    position: { left, top, width, height },
    fill: fill === "none" ? "none" : fill,
    line: line === "none" ? { fill: "none", width: 0 } : line,
  });
}

function text(slide, value, left, top, width, height, options = {}) {
  const box = shape(slide, "textbox", left, top, width, height);
  box.text = value;
  box.text.style = {
    typeface: font,
    fontSize: options.size ?? 24,
    bold: options.bold ?? false,
    color: options.color ?? C.ink,
    alignment: options.align ?? "left",
    verticalAlignment: options.vertical ?? "middle",
    autoFit: "shrinkText",
    lineSpacing: options.lineSpacing ?? 1.0,
  };
  return box;
}

function title(slide, value, index, dark = false) {
  text(slide, value, 70, 42, 1030, 66, { size: 36, bold: true, color: dark ? C.white : C.navy });
  text(slide, String(index).padStart(2, "0"), 1150, 48, 60, 38, {
    size: 15, bold: true, color: dark ? C.mint : C.teal, align: "right",
  });
}

function note(slide, value) {
  slide.speakerNotes.textFrame.setText(value);
}

function line(slide, left, top, width, height, color = C.line, weight = 2) {
  return shape(slide, "line", left, top, width, height, "none", { fill: color, width: weight });
}

function label(slide, value, left, top, width, color = C.teal) {
  return text(slide, value.toUpperCase(), left, top, width, 28, { size: 14, bold: true, color });
}

// 1. Capa
{
  const s = deck.slides.add();
  s.background.fill = C.navy;
  shape(s, "rect", 0, 0, 24, 720, C.teal);
  shape(s, "ellipse", 930, 70, 270, 270, { color: C.teal, transparency: 18 });
  shape(s, "ellipse", 1010, 180, 190, 190, { color: C.mint, transparency: 12 });
  shape(s, "ellipse", 880, 300, 320, 320, { color: C.navy2, transparency: 5 }, { fill: C.teal, width: 3 });
  label(s, "Planejamento financeiro pessoal", 78, 105, 520, C.mint);
  text(s, "FinFlow", 72, 180, 700, 105, { size: 62, bold: true, color: C.white });
  text(s, "Um backend que transforma dados financeiros em um plano diário claro e auditável", 76, 294, 690, 132, {
    size: 28, color: C.cream, lineSpacing: 1.08,
  });
  text(s, "Produto e arquitetura", 78, 590, 390, 34, { size: 17, color: C.mint });
  text(s, "Kotlin  •  Spring Boot  •  PostgreSQL", 78, 628, 520, 28, { size: 15, color: C.white });
  note(s, "Abra apresentando o FinFlow como um planejador financeiro pessoal. O produto consolida dados, calcula o que realmente está disponível e gera ações que o usuário pode revisar. A apresentação usa apenas capacidades presentes no repositório.");
}

// 2. Problema
{
  const s = deck.slides.add();
  s.background.fill = C.cream;
  title(s, "A decisão financeira acontece com dados espalhados", 2);
  text(s, "Saldo bancário", 78, 160, 260, 48, { size: 26, bold: true, color: C.navy });
  text(s, "mostra o agora", 78, 210, 260, 34, { size: 18, color: C.muted });
  text(s, "Faturas e dívidas", 78, 305, 300, 48, { size: 26, bold: true, color: C.navy });
  text(s, "mostram compromissos", 78, 355, 300, 34, { size: 18, color: C.muted });
  text(s, "Metas e reserva", 78, 450, 300, 48, { size: 26, bold: true, color: C.navy });
  text(s, "mostram o futuro", 78, 500, 300, 34, { size: 18, color: C.muted });
  line(s, 420, 140, 0, 440, C.line, 3);
  label(s, "Pergunta que fica sem resposta", 500, 158, 500, C.coral);
  text(s, "Quanto posso gastar hoje sem comprometer o restante do mês?", 495, 215, 660, 170, {
    size: 38, bold: true, color: C.navy, lineSpacing: 1.04,
  });
  shape(s, "rect", 500, 440, 600, 4, C.teal);
  text(s, "O FinFlow reúne essas informações antes de recomendar qualquer ação.", 500, 475, 610, 90, {
    size: 23, color: C.ink,
  });
  note(s, "O problema central não é a falta de telas bancárias. É a falta de uma resposta consolidada para a decisão diária. Saldo isolado não considera obrigações, dívida cara, reserva e metas.");
}

// 3. Proposta de valor
{
  const s = deck.slides.add();
  s.background.fill = C.white;
  title(s, "Um CFO pessoal baseado em regras", 3);
  label(s, "Entrada", 78, 138, 220);
  text(s, "Contas, transações, obrigações, dívidas, metas e carteira", 78, 178, 430, 130, {
    size: 25, bold: true, color: C.navy,
  });
  line(s, 565, 178, 0, 350, C.line, 2);
  label(s, "Cálculo", 635, 138, 220);
  text(s, "O motor protege o caixa do mês e distribui somente o excedente disponível.", 635, 178, 500, 130, {
    size: 25, bold: true, color: C.navy,
  });
  shape(s, "rect", 76, 390, 1058, 170, C.navy);
  text(s, "Resultado", 110, 414, 170, 28, { size: 16, bold: true, color: C.mint });
  text(s, "saldo livre real", 110, 460, 260, 52, { size: 27, bold: true, color: C.white });
  text(s, "limite diário", 450, 460, 220, 52, { size: 27, bold: true, color: C.white });
  text(s, "próxima ação", 770, 460, 240, 52, { size: 27, bold: true, color: C.white });
  text(s, "Cada plano mantém avisos, intenções e registro de auditoria.", 78, 608, 760, 42, { size: 20, color: C.muted });
  note(s, "Explique que o FinFlow não tenta prever o mercado. Ele aplica regras financeiras explícitas e devolve um plano que pode ser entendido, revisado e auditado.");
}

// 4. Ordem financeira
{
  const s = deck.slides.add();
  s.background.fill = C.cream;
  title(s, "A ordem de prioridade protege o caixa", 4);
  const levels = [
    { y: 150, w: 980, x: 150, c: C.navy, n: "1", h: "Obrigações e caixa mínimo", d: "Reserva o que vence antes da próxima renda" },
    { y: 252, w: 840, x: 220, c: C.navy2, n: "2", h: "Dívida de alto custo", d: "Usa o excedente para reduzir juros caros" },
    { y: 354, w: 700, x: 290, c: C.teal, n: "3", h: "Reserva de emergência", d: "Avança até a meta configurada" },
    { y: 456, w: 560, x: 360, c: "#56BFAF", n: "4", h: "Investimentos", d: "Aporte após as prioridades anteriores" },
  ];
  for (const l of levels) {
    shape(s, "roundRect", l.x, l.y, l.w, 82, l.c);
    text(s, l.n, l.x + 24, l.y + 15, 46, 50, { size: 25, bold: true, color: C.white, align: "center" });
    text(s, l.h, l.x + 90, l.y + 10, Math.min(340, l.w * 0.48), 34, { size: 23, bold: true, color: C.white });
    text(s, l.d, l.x + l.w * 0.62, l.y + 12, l.w * 0.34, 54, { size: 16, color: C.white, align: "right" });
  }
  text(s, "O limite diário nasce do saldo livre dividido pelos dias até a próxima renda.", 215, 598, 850, 54, {
    size: 22, bold: true, color: C.navy, align: "center",
  });
  note(s, "Esta é a regra principal do produto. O investimento aparece no fim porque o sistema primeiro cobre o mês, reduz dívida cara e recompõe a reserva.");
}

// 5. Jornada da API
{
  const s = deck.slides.add();
  s.background.fill = C.white;
  title(s, "A jornada até o primeiro plano", 5);
  const steps = [
    ["01", "Perfil", "renda, orçamento e risco"],
    ["02", "Contas", "saldos por finalidade"],
    ["03", "Compromissos", "obrigações e dívidas"],
    ["04", "Transações", "importação idempotente"],
    ["05", "Plano", "cálculo e intenções"],
  ];
  const xs = [65, 310, 555, 800, 1045];
  line(s, 120, 270, 1020, 0, C.line, 5);
  for (let i = 0; i < steps.length; i++) {
    const [n, h, d] = steps[i];
    shape(s, "ellipse", xs[i], 225, 110, 110, i === steps.length - 1 ? C.teal : C.navy);
    text(s, n, xs[i], 249, 110, 54, { size: 25, bold: true, color: C.white, align: "center" });
    text(s, h, xs[i] - 35, 365, 180, 42, { size: 22, bold: true, color: C.navy, align: "center" });
    text(s, d, xs[i] - 45, 412, 200, 74, { size: 16, color: C.muted, align: "center" });
  }
  shape(s, "rect", 165, 550, 950, 2, C.line);
  text(s, "Depois do plano, o usuário aprova ou rejeita cada intenção. O backend não executa transferências.", 165, 575, 950, 62, {
    size: 21, color: C.ink, align: "center",
  });
  note(s, "A jornada começa pela configuração do perfil e termina com um plano revisável. A importação exige Idempotency-Key, evitando que o mesmo lote seja processado duas vezes.");
}

// 6. Capacidades
{
  const s = deck.slides.add();
  s.background.fill = C.cream;
  title(s, "O MVP cobre o ciclo financeiro essencial", 6);
  const rows = [
    ["Consolidar", "Contas, saldos e transações em um modelo canônico"],
    ["Planejar", "Obrigações, orçamento variável, dívida, reserva e aportes"],
    ["Acompanhar", "Metas, carteira e relatório mensal"],
    ["Controlar", "Consentimentos, API key, auditoria e erros padronizados"],
  ];
  for (let i = 0; i < rows.length; i++) {
    const y = 145 + i * 118;
    text(s, rows[i][0], 80, y, 230, 62, { size: 27, bold: true, color: i === 3 ? C.coral : C.teal });
    text(s, rows[i][1], 355, y, 790, 62, { size: 22, color: C.ink });
    if (i < rows.length - 1) line(s, 80, y + 90, 1060, 0, C.line, 1);
  }
  text(s, "Modos disponíveis: OBSERVER, COPILOT e AUTOPILOT. No MVP, nenhum modo libera execução bancária.", 80, 628, 1040, 38, {
    size: 17, color: C.muted,
  });
  note(s, "O MVP já cobre cadastro, consolidação, planejamento, acompanhamento e controles técnicos. Os três modos existem no perfil, mas a fronteira de execução continua fechada.");
}

// 7. Segurança
{
  const s = deck.slides.add();
  s.background.fill = C.navy;
  title(s, "Recomendação e execução permanecem separadas", 7, true);
  const items = [
    [80, "Dados canônicos", "contas e transações"],
    [340, "Motor", "regras determinísticas"],
    [600, "Intenção", "ação proposta"],
    [860, "Decisão humana", "aprovar ou rejeitar"],
  ];
  line(s, 170, 308, 780, 0, C.mint, 4);
  for (const [x, h, d] of items) {
    shape(s, "roundRect", x, 235, 210, 150, x === 860 ? C.teal : C.navy2, { fill: C.mint, width: 2 });
    text(s, h, x + 18, 260, 174, 42, { size: 21, bold: true, color: C.white, align: "center" });
    text(s, d, x + 18, 312, 174, 44, { size: 15, color: C.mint, align: "center" });
  }
  shape(s, "roundRect", 390, 470, 500, 92, { color: C.coral, transparency: 6 });
  text(s, "Sem executor bancário no MVP", 410, 489, 460, 50, { size: 25, bold: true, color: C.white, align: "center" });
  text(s, "Aprovar muda o estado da intenção. O campo executionAvailable permanece falso.", 230, 610, 820, 38, {
    size: 18, color: C.cream, align: "center",
  });
  note(s, "Este é o limite de confiança do produto atual. A aplicação recomenda e registra a decisão, mas não movimenta dinheiro. Uma integração futura precisará passar por um orquestrador determinístico e um conector regulado.");
}

// 8. Arquitetura e resiliência
{
  const s = deck.slides.add();
  s.background.fill = C.white;
  title(s, "Arquitetura preparada para evoluir", 8);
  label(s, "Monólito modular", 80, 132, 260);
  text(s, "Uma implantação com fronteiras por domínio", 80, 169, 430, 72, { size: 27, bold: true, color: C.navy });
  const modules = ["profile", "account", "transaction", "planning", "portfolio", "openfinance", "report"];
  modules.forEach((m, i) => {
    const col = i % 4;
    const row = Math.floor(i / 4);
    shape(s, "roundRect", 80 + col * 112, 280 + row * 72, 98, 48, C.pale);
    text(s, m, 84 + col * 112, 289 + row * 72, 90, 30, { size: 14, bold: true, color: C.navy, align: "center" });
  });
  line(s, 570, 135, 0, 455, C.line, 2);
  label(s, "Resiliência", 635, 132, 260, C.coral);
  const resilience = [
    ["Idempotência", "repetição segura na importação"],
    ["Persistência", "Flyway e restrições de unicidade"],
    ["Erros", "contrato RFC 9457 sem detalhes internos"],
    ["Auditoria", "eventos para mudanças relevantes"],
  ];
  resilience.forEach((r, i) => {
    const y = 178 + i * 105;
    text(s, r[0], 635, y, 200, 36, { size: 22, bold: true, color: C.navy });
    text(s, r[1], 845, y, 330, 44, { size: 17, color: C.muted });
    if (i < resilience.length - 1) line(s, 635, y + 72, 540, 0, C.line, 1);
  });
  text(s, "Stack: Java 17, Kotlin 2.3.21, Spring Boot 4.1.0, PostgreSQL 17 e Gradle 9.5.1", 80, 625, 1080, 34, {
    size: 16, color: C.muted,
  });
  note(s, "O monólito modular reduz o custo operacional do MVP sem misturar os domínios. A resiliência atual inclui idempotência, migrations, restrições no banco, auditoria e respostas de erro estáveis.");
}

// 9. Próximos passos
{
  const s = deck.slides.add();
  s.background.fill = C.cream;
  title(s, "Próximos passos do produto", 9);
  const phases = [
    { x: 80, n: "Agora", h: "MVP operacional", d: "Planejamento, relatórios e intenções auditáveis", c: C.navy },
    { x: 425, n: "Próximo", h: "Open Finance real", d: "OAuth, sincronização paginada, webhooks e tokens protegidos", c: C.teal },
    { x: 770, n: "Depois", h: "Execução controlada", d: "Outbox, reconciliação, limites e conectores regulados", c: C.coral },
  ];
  for (const p of phases) {
    label(s, p.n, p.x, 160, 260, p.c);
    text(s, p.h, p.x, 207, 285, 58, { size: 27, bold: true, color: C.navy });
    text(s, p.d, p.x, 280, 285, 130, { size: 19, color: C.ink });
    shape(s, "rect", p.x, 438, 285, 5, p.c);
  }
  line(s, 80, 510, 975, 0, C.line, 2);
  text(s, "FinFlow já demonstra a lógica central do produto: transformar dados financeiros em decisões explicáveis, sem abrir mão de controle.", 125, 555, 920, 88, {
    size: 26, bold: true, color: C.navy, align: "center",
  });
  note(s, "Feche mostrando que o MVP prova a lógica central. O próximo salto é trocar o registro local de consentimento por uma integração real, mantendo a separação entre recomendação, autorização e execução.");
}

const stagingDir = path.join(workspaceDir, ".codex", "presentation-build", "finalizer");
await fs.mkdir(stagingDir, { recursive: true });
await fs.mkdir(path.dirname(FINAL_PPTX), { recursive: true });
const candidatePath = path.join(stagingDir, "finflow-candidate.pptx");
await (await PresentationFile.exportPptx(deck)).save(candidatePath);

const result = await finalizePresentation({
  workspaceDir,
  candidatePath,
  finalPath: FINAL_PPTX,
  pythonExecutable: RUNTIME_PYTHON,
  integrityValidatorPath: path.join(SKILL_DIR, "container_tools", "inspect_presentation_package_integrity.py"),
  layoutValidatorPath: path.join(SKILL_DIR, "container_tools", "inspect_presentation_layout_geometry.py"),
  layoutArgs: [
    "--expected-slide-size-emu", "12192000,6858000",
    "--validate-bullet-geometry",
    "--validate-heading-fit",
  ],
  explicitTotalSlideCount: 9,
  requiredNativeTableOwnerSlides: [],
  requiredNativeChartOwnerSlides: [],
  fontPolicy: { basis: "design", families: [font] },
  verifyArtifactToolImport: true,
  receiptPath: path.join(stagingDir, "FinFlow-apresentacao-produto-final.validation.json"),
});

console.log(JSON.stringify({ finalPath: FINAL_PPTX, font, result }, null, 2));
