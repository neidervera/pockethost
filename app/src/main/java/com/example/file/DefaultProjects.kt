package com.example.file

import java.io.File

object DefaultProjects {

    fun populateDefaultProjects(fileManager: FileManager) {
        val defaultDir = fileManager.getProjectDir("default")
        installModernDashboard(defaultDir)
    }

    fun resetDemoProject(targetDir: File) {
        targetDir.deleteRecursively()
        targetDir.mkdirs()
        installModernDashboard(targetDir)
    }

    private fun installModernDashboard(dir: File) {
        dir.mkdirs()
        WebTemplates.installWebFileManager(dir)
        WebTemplates.installCodeRunnerWeb(dir)

        // 1. index.html - Unified Web Portal with the two requested menus directly on the home page!
        File(dir, "index.html").writeText(
            """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PocketHost Server</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .menu-nav {
            display: flex;
            gap: 12px;
            margin: 16px 0;
            border-bottom: 2px solid var(--border-color);
            padding-bottom: 12px;
        }
        .menu-btn {
            background: #161b22;
            border: 1px solid var(--border-color);
            color: var(--text-muted);
            padding: 12px 24px;
            border-radius: 8px;
            font-size: 1rem;
            font-weight: bold;
            cursor: pointer;
            transition: all 0.2s;
        }
        .menu-btn.active {
            background: rgba(0, 229, 255, 0.15);
            border-color: var(--accent);
            color: var(--accent);
        }
        .menu-section {
            display: none;
        }
        .menu-section.active {
            display: block;
        }
        .dropzone {
            border: 2px dashed var(--accent);
            background: rgba(0, 229, 255, 0.05);
            border-radius: 12px;
            padding: 24px;
            text-align: center;
            cursor: pointer;
            margin-bottom: 16px;
        }
        .file-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 12px;
        }
        .file-table th {
            text-align: left;
            padding: 10px;
            border-bottom: 2px solid var(--border-color);
            color: var(--text-muted);
            font-size: 0.85rem;
        }
        .file-table td {
            padding: 12px 10px;
            border-bottom: 1px solid #1c2333;
            font-size: 0.9rem;
        }
        .file-table tr:hover {
            background: rgba(255,255,255,0.03);
        }
        .btn-action {
            display: inline-flex;
            align-items: center;
            padding: 6px 12px;
            border-radius: 6px;
            font-size: 0.8rem;
            font-weight: 600;
            text-decoration: none;
            cursor: pointer;
            border: none;
            transition: 0.2s;
        }
        .btn-download {
            background: rgba(0, 230, 118, 0.15);
            color: var(--emerald);
            border: 1px solid var(--emerald);
        }
        .btn-download:hover {
            background: var(--emerald);
            color: #0b0f17;
        }
        .btn-view {
            background: rgba(0, 229, 255, 0.15);
            color: var(--accent);
            border: 1px solid var(--accent);
            margin-right: 6px;
        }
        .btn-view:hover {
            background: var(--accent);
            color: #0b0f17;
        }
        .btn-del {
            background: rgba(255, 51, 102, 0.15);
            color: #ff3366;
            border: 1px solid #ff3366;
            margin-left: 6px;
        }
        .btn-del:hover {
            background: #ff3366;
            color: #fff;
        }
        .lang-tabs {
            display: flex;
            gap: 10px;
            margin-bottom: 14px;
        }
        .lang-tab {
            padding: 8px 18px;
            border-radius: 6px;
            background: #161b22;
            border: 1px solid var(--border-color);
            color: var(--text-muted);
            cursor: pointer;
            font-weight: bold;
            font-size: 0.9rem;
        }
        .lang-tab.active {
            background: rgba(0, 229, 255, 0.15);
            border-color: var(--accent);
            color: var(--accent);
        }
        .editor-container {
            background: #06090e;
            border: 1px solid var(--border-color);
            border-radius: 8px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            margin-bottom: 14px;
        }
        .editor-bar {
            background: #0d1117;
            padding: 8px 14px;
            display: flex;
            justify-content: space-between;
            border-bottom: 1px solid #21262d;
            font-size: 0.85rem;
            color: var(--text-muted);
        }
        #code-editor {
            width: 100%;
            height: 280px;
            background: #06090e;
            color: #f0f6fc;
            font-family: monospace;
            font-size: 0.95rem;
            line-height: 1.5;
            padding: 12px;
            border: none;
            outline: none;
            resize: vertical;
        }
        .terminal {
            background: #05070a;
            border: 1px solid #21262d;
            border-radius: 8px;
            padding: 14px;
            font-family: monospace;
            font-size: 0.9rem;
            min-height: 140px;
            max-height: 350px;
            overflow-y: auto;
        }
        .term-header {
            display: flex;
            justify-content: space-between;
            color: var(--text-muted);
            font-size: 0.8rem;
            margin-bottom: 8px;
            border-bottom: 1px solid #1c222d;
            padding-bottom: 4px;
        }
        .modal {
            display: none;
            position: fixed;
            top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(0,0,0,0.85);
            backdrop-filter: blur(8px);
            z-index: 1000;
            align-items: center;
            justify-content: center;
            padding: 16px;
        }
        .modal-content {
            background: #121824;
            border: 1px solid var(--border-color);
            border-radius: 12px;
            width: 100%;
            max-width: 760px;
            max-height: 85vh;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }
        .modal-header {
            padding: 14px 18px;
            border-bottom: 1px solid var(--border-color);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .modal-body {
            padding: 16px;
            overflow: auto;
            flex: 1;
            display: flex;
            justify-content: center;
            align-items: center;
            background: #090d14;
        }
        .modal-body img, .modal-body video {
            max-width: 100%;
            max-height: 60vh;
            border-radius: 6px;
        }
        .modal-body pre {
            width: 100%;
            color: #e6edf3;
            font-family: monospace;
            font-size: 0.85rem;
            white-space: pre-wrap;
            word-break: break-all;
        }
    </style>
</head>
<body>
    <div class="glass-container">
        <header>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <span style="color: var(--accent); font-weight: bold; font-size: 0.85rem;">PocketHost Server</span>
                <span style="color: var(--emerald); font-weight: bold; font-size: 0.85rem;">CONECTADO A WI-FI</span>
            </div>
            <h1>Panel del Servidor Local</h1>
            <p class="subtitle">Dispositivo: {{device_model}} | Tu IP: {{client_ip}} | Tiempo activo: {{server_uptime}}</p>
        </header>

        <!-- Main Menus Navigation -->
        <nav class="menu-nav">
            <button id="nav-btn-files" class="menu-btn active" onclick="switchTab('files')">
                Archivos (Ver, Subir, Descargar)
            </button>
            <button id="nav-btn-runner" class="menu-btn" onclick="switchTab('runner')">
                Ejecutar Python / C++
            </button>
        </nav>

        <!-- MENU 1: ARCHIVOS (SUBIR, VER, DESCARGAR) -->
        <main id="section-files" class="menu-section active">
            <section class="card">
                <h2>Subir Archivos al Servidor</h2>
                <div id="dropzone" class="dropzone">
                    <p style="font-size: 1rem; font-weight: bold; color: var(--accent); margin-bottom: 4px;">
                        Selecciona o arrastra archivos aqui
                    </p>
                    <p style="font-size: 0.8rem; color: var(--text-muted);">
                        Soporta HTML, CSS, JS, Python, C++, imagenes, videos, documentos
                    </p>
                    <input type="file" id="file-input" multiple style="display: none;">
                </div>
                <div id="upload-status" style="font-size: 0.85rem; color: var(--accent);"></div>
            </section>

            <section class="card">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                    <h2>Archivos Alojados</h2>
                    <div style="display: flex; gap: 8px;">
                        <button onclick="createNewFolder()" class="btn">Nueva Carpeta</button>
                        <button onclick="loadFiles()" class="btn">Actualizar</button>
                    </div>
                </div>

                <div style="overflow-x: auto;">
                    <table class="file-table">
                        <thead>
                            <tr>
                                <th>Nombre</th>
                                <th>Tamaño</th>
                                <th>Modificado</th>
                                <th style="text-align: right;">Acciones</th>
                            </tr>
                        </thead>
                        <tbody id="files-tbody">
                            <tr><td colspan="4" style="text-align: center; color: var(--text-muted);">Cargando archivos...</td></tr>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>

        <!-- MENU 2: EJECUTOR PYTHON / C++ -->
        <main id="section-runner" class="menu-section">
            <section class="card">
                <h2>Ejecutor de Codigo Python y C++</h2>
                <p style="color: var(--text-muted); font-size: 0.85rem; margin-bottom: 14px;">
                    Ejecuta scripts directamente en el servidor. Si falta una libreria se descarga automaticamente.
                </p>

                <div class="lang-tabs">
                    <button id="lang-btn-py" class="lang-tab active" onclick="setRunnerLang('python')">
                        Python 3
                    </button>
                    <button id="lang-btn-cpp" class="lang-tab" onclick="setRunnerLang('cpp')">
                        C++ (GCC)
                    </button>
                </div>

                <div style="display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap;">
                    <span style="font-size: 0.85rem; color: var(--text-muted); align-self: center;">Plantillas:</span>
                    <button onclick="loadSnippet(1)" class="btn" style="padding: 4px 10px; font-size: 0.8rem;">Basico</button>
                    <button onclick="loadSnippet(2)" class="btn" style="padding: 4px 10px; font-size: 0.8rem;">Calculo / Algoritmo</button>
                    <button id="snippet-lib-btn" onclick="loadSnippet(3)" class="btn" style="padding: 4px 10px; font-size: 0.8rem;">Prueba Libreria (requests)</button>
                    <button onclick="loadSnippet(4)" class="btn" style="padding: 4px 10px; font-size: 0.8rem; border-color: var(--emerald); color: var(--emerald);">Bucle 24/7</button>
                    <button onclick="loadSnippet(5)" class="btn" style="padding: 4px 10px; font-size: 0.8rem; border-color: var(--accent); color: var(--accent);">Bot Discord 24/7</button>
                </div>

                <div class="editor-container">
                    <div class="editor-bar">
                        <span id="runner-filename">script.py</span>
                        <span>Ctrl + Enter para ejecutar</span>
                    </div>
                    <textarea id="code-editor" spellcheck="false"></textarea>
                </div>

                <div style="margin-bottom: 14px;">
                    <label style="font-size: 0.85rem; color: var(--text-muted); display: block; margin-bottom: 4px;">Entrada estandar (stdin) opcional:</label>
                    <input type="text" id="stdin-input" placeholder="Datos de entrada para el programa..." style="width: 100%; background: #06090e; border: 1px solid var(--border-color); color: #fff; padding: 10px; border-radius: 6px; font-family: monospace;">
                </div>

                <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; margin-bottom: 14px;">
                    <label style="display: inline-flex; align-items: center; gap: 8px; font-size: 0.9rem; color: var(--emerald); font-weight: bold; cursor: pointer;">
                        <input type="checkbox" id="auto-install-checkbox" checked>
                        <span>Descargar librerias automaticamente si faltan (pip / PyPI)</span>
                    </label>

                    <div style="display: flex; gap: 8px;">
                        <button onclick="saveCodeToFile()" class="btn">Guardar en Servidor</button>
                        <button id="run-code-btn" onclick="runCode()" class="btn primary" style="font-size: 0.95rem; padding: 10px 22px;">
                            Ejecutar Una Vez
                        </button>
                    </div>
                </div>

                <!-- 24/7 Execution Controls -->
                <div style="background: rgba(0, 229, 255, 0.05); border: 1px solid var(--border-color); border-radius: 8px; padding: 12px; margin-bottom: 14px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px;">
                    <div>
                        <strong style="color: var(--accent); font-size: 0.9rem; display: block;">Modo 24/7 en Segundo Plano</strong>
                        <span style="color: var(--text-muted); font-size: 0.8rem;">Ejecuta el script de forma continua en el celular aunque cierres el navegador.</span>
                    </div>
                    <div style="display: flex; gap: 8px; align-items: center;">
                        <span id="daemon-badge" style="font-size: 0.85rem; color: var(--text-muted); margin-right: 6px;">Estado: Inactivo</span>
                        <button id="daemon-start-btn" onclick="startDaemon()" class="btn" style="background: rgba(0, 230, 118, 0.2); border-color: var(--emerald); color: var(--emerald); font-weight: bold;">
                            Ejecutar 24/7
                        </button>
                        <button id="daemon-stop-btn" onclick="stopDaemon()" class="btn" style="background: rgba(255, 51, 102, 0.2); border-color: #ff3366; color: #ff3366; display: none; font-weight: bold;">
                            Detener 24/7
                        </button>
                    </div>
                </div>

                <div class="terminal">
                    <div class="term-header">
                        <span>CONSOLA DE SALIDA</span>
                        <span id="runner-status">Listo</span>
                    </div>
                    <div id="stdout-area" style="color: #58a6ff; white-space: pre-wrap; word-break: break-all;">// La salida del programa aparecera aqui...</div>
                    <div id="stderr-area" style="color: #ff3366; white-space: pre-wrap; word-break: break-all; margin-top: 6px;"></div>
                </div>
            </section>
        </main>

        <footer>
            <p>PocketHost Server en Android • Red Wi-Fi Local</p>
        </footer>
    </div>

    <!-- Preview Modal -->
    <div id="view-modal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h3 id="modal-title" style="color: #fff; font-size: 1rem;">Vista Previa</h3>
                <button onclick="closeViewModal()" class="btn" style="padding: 4px 10px;">Cerrar</button>
            </div>
            <div id="modal-body" class="modal-body"></div>
            <div style="padding: 10px 16px; border-top: 1px solid var(--border-color); display: flex; justify-content: flex-end; gap: 8px;">
                <a id="modal-dl" href="#" class="btn-action btn-download" download>Descargar</a>
                <button onclick="closeViewModal()" class="btn">Cerrar</button>
            </div>
        </div>
    </div>

    <script src="app.js"></script>
</body>
</html>
            """.trimIndent()
        )

        // 2. style.css - Clean, professional dark theme without emojis
        File(dir, "style.css").writeText(
            """
:root {
    --bg-color: #0b0f17;
    --card-bg: rgba(22, 27, 34, 0.9);
    --border-color: #30363d;
    --accent: #00e5ff;
    --emerald: #00e676;
    --text-main: #f0f6fc;
    --text-muted: #8b949e;
}

* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

body {
    background-color: var(--bg-color);
    color: var(--text-main);
    min-height: 100vh;
    padding: 20px 14px;
    display: flex;
    justify-content: center;
}

.glass-container {
    width: 100%;
    max-width: 860px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

header {
    text-align: left;
    padding: 12px 0;
    border-bottom: 1px solid #21262d;
}

h1 {
    font-size: 1.8rem;
    font-weight: 800;
    color: #fff;
    margin-bottom: 4px;
}

.subtitle {
    color: var(--text-muted);
    font-size: 0.9rem;
}

.card {
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 20px;
}

.card h2 {
    font-size: 1.15rem;
    margin-bottom: 8px;
    color: var(--text-main);
}

.btn {
    background: rgba(0, 229, 255, 0.1);
    color: var(--accent);
    border: 1px solid var(--accent);
    padding: 8px 16px;
    border-radius: 6px;
    font-weight: 600;
    font-size: 0.85rem;
    cursor: pointer;
    transition: all 0.2s;
}

.btn:hover {
    background: var(--accent);
    color: #0b0f17;
}

.btn.primary {
    background: var(--accent);
    color: #0b0f17;
    font-weight: bold;
}

footer {
    text-align: center;
    padding: 16px 0;
    color: var(--text-muted);
    font-size: 0.8rem;
}
            """.trimIndent()
        )

        // 3. app.js - Full script handling Tab navigation, File Manager, and Code Execution
        File(dir, "app.js").writeText(
            """
// Tab switching
function switchTab(tab) {
    document.getElementById('section-files').classList.toggle('active', tab === 'files');
    document.getElementById('section-runner').classList.toggle('active', tab === 'runner');
    document.getElementById('nav-btn-files').classList.toggle('active', tab === 'files');
    document.getElementById('nav-btn-runner').classList.toggle('active', tab === 'runner');
    if (tab === 'files') loadFiles();
}

// ---------------- FILE MANAGER ----------------
async function loadFiles() {
    const tbody = document.getElementById('files-tbody');
    tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--text-muted);">Cargando archivos...</td></tr>';
    try {
        const res = await fetch('/api/files');
        const data = await res.json();
        const files = data.files || [];

        if (files.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--text-muted); padding: 18px;">No hay archivos en el servidor. Sube uno arriba.</td></tr>';
            return;
        }

        let html = '';
        for (const f of files) {
            const tag = f.is_dir ? '[CARPETA]' : '[' + (f.extension.toUpperCase() || 'FILE') + ']';
            const date = new Date(f.modified).toLocaleString();
            const viewBtn = !f.is_dir 
                ? '<button onclick="viewFile(\'' + f.path + '\', \'' + f.mime + '\', \'' + f.name + '\')" class="btn-action btn-view">Ver</button>' 
                : '';
            const downloadBtn = !f.is_dir
                ? '<a href="/api/download?file=' + encodeURIComponent(f.path) + '" class="btn-action btn-download">Descargar</a>'
                : '';
            const deleteBtn = '<button onclick="deleteFile(\'' + f.path + '\', \'' + f.name + '\')" class="btn-action btn-del">Eliminar</button>';

            html += '<tr>' +
                '<td><span style="color: var(--accent); font-family: monospace; font-size: 0.8rem; margin-right: 6px;">' + tag + '</span> <strong>' + escapeHtml(f.name) + '</strong></td>' +
                '<td>' + (f.is_dir ? '--' : f.formatted_size) + '</td>' +
                '<td style="color: var(--text-muted); font-size: 0.8rem;">' + date + '</td>' +
                '<td style="text-align: right;">' + viewBtn + downloadBtn + deleteBtn + '</td>' +
                '</tr>';
        }
        tbody.innerHTML = html;
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: #ff3366;">Error al cargar archivos: ' + err.message + '</td></tr>';
    }
}

function escapeHtml(str) {
    return str.replace(/[&<>"']/g, m => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;' })[m]);
}

async function viewFile(path, mime, name) {
    const modal = document.getElementById('view-modal');
    const title = document.getElementById('modal-title');
    const body = document.getElementById('modal-body');
    const dlBtn = document.getElementById('modal-dl');

    title.textContent = 'Archivo: ' + name;
    dlBtn.href = '/api/download?file=' + encodeURIComponent(path);
    dlBtn.setAttribute('download', name);
    body.innerHTML = '<p style="color: var(--accent);">Cargando vista previa...</p>';
    modal.style.display = 'flex';

    const streamUrl = '/' + path.split('/').map(encodeURIComponent).join('/');
    const previewUrl = '/api/preview?file=' + encodeURIComponent(path);

    if (mime.startsWith('image/')) {
        body.innerHTML = '<img src="' + previewUrl + '" alt="' + escapeHtml(name) + '" style="max-width:100%; max-height:65vh; border-radius:8px; object-fit:contain;">';
    } else if (mime.startsWith('video/')) {
        body.innerHTML = '<div style="width:100%; text-align:center;"><video controls autoplay playsinline style="width:100%; max-height:65vh; border-radius:8px; background:#000;" src="' + streamUrl + '"><p>Tu navegador no soporta reproduccion directa de video.</p></video><p style="margin-top:8px; font-size:0.8rem; color:var(--text-muted);">' + escapeHtml(name) + '</p></div>';
    } else if (mime.startsWith('audio/')) {
        body.innerHTML = '<div style="width:100%; max-width:480px; text-align:center; padding:28px 18px; background:#161b22; border-radius:10px; border:1px solid var(--border-color);"><p style="font-weight:bold; color:var(--text-main); margin-bottom:16px;">' + escapeHtml(name) + '</p><audio controls autoplay style="width:100%;" src="' + streamUrl + '"><p>Tu navegador no soporta reproduccion directa de audio.</p></audio></div>';
    } else {
        try {
            const res = await fetch(previewUrl);
            const text = await res.text();
            body.innerHTML = '<pre style="max-height:60vh; overflow:auto; text-align:left; width:100%; color:#e6edf3; font-family:monospace; font-size:0.85rem; white-space:pre-wrap; word-break:break-all;">' + escapeHtml(text.slice(0, 80000)) + '</pre>';
        } catch (e) {
            body.innerHTML = '<p style="color: #ff3366;">No se pudo previsualizar el archivo.</p>';
        }
    }
}

function closeViewModal() {
    const modal = document.getElementById('view-modal');
    modal.style.display = 'none';
    const video = modal.querySelector('video');
    if (video) video.pause();
    const audio = modal.querySelector('audio');
    if (audio) audio.pause();
    document.getElementById('modal-body').innerHTML = '';
}

async function deleteFile(path, name) {
    if (!confirm('Deseas eliminar permanentemente "' + name + '" del servidor?')) return;
    try {
        const res = await fetch('/api/delete?file=' + encodeURIComponent(path), { method: 'POST' });
        const json = await res.json();
        if (json.success) {
            loadFiles();
        } else {
            alert('Error al eliminar el archivo');
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

async function createNewFolder() {
    const name = prompt('Nombre de la nueva carpeta:');
    if (!name) return;
    try {
        const res = await fetch('/api/create_folder?name=' + encodeURIComponent(name), { method: 'POST' });
        const json = await res.json();
        if (json.success) loadFiles();
    } catch (e) {
        alert('Error al crear carpeta: ' + e.message);
    }
}

// Upload setup
const dropzone = document.getElementById('dropzone');
const fileInput = document.getElementById('file-input');

if (dropzone && fileInput) {
    dropzone.addEventListener('click', () => fileInput.click());
    dropzone.addEventListener('dragover', (e) => { e.preventDefault(); });
    dropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        if (e.dataTransfer.files.length) uploadFiles(e.dataTransfer.files);
    });
    fileInput.addEventListener('change', () => {
        if (fileInput.files.length) uploadFiles(fileInput.files);
    });
}

async function uploadFiles(files) {
    const status = document.getElementById('upload-status');
    for (let i = 0; i < files.length; i++) {
        const f = files[i];
        status.textContent = 'Subiendo (' + (i+1) + '/' + files.length + '): ' + f.name + '...';
        try {
            await fetch('/api/upload?name=' + encodeURIComponent(f.name), { method: 'POST', body: f });
        } catch (e) {
            status.textContent = 'Error al subir ' + f.name;
            return;
        }
    }
    status.textContent = 'Archivos subidos exitosamente.';
    setTimeout(() => { status.textContent = ''; }, 2500);
    fileInput.value = '';
    loadFiles();
}

// ---------------- CODE RUNNER (PYTHON / C++) ----------------
let currentLang = 'python';

const runnerSnippets = {
    python: {
        1: '# Hola Mundo en Python 3\nimport sys, platform\nprint("Ejecutando Python en PocketHost Server")\nprint("Version de Python:", sys.version.split()[0])\nprint("Sistema:", platform.system(), platform.machine())',
        2: '# Calculo de numeros primos y algoritmo\ndef es_primo(n):\n    if n < 2: return False\n    for i in range(2, int(n**0.5) + 1):\n        if n % i == 0: return False\n    return True\n\nprimos = [n for n in range(2, 60) if es_primo(n)]\nprint("Numeros primos menores a 60:")\nprint(primos)',
        3: '# Auto-descarga automatica de librerias externas\n# Si no existe, el servidor la descarga automaticamente via PyPI!\nimport requests\n\nprint("Realizando consulta HTTP con la libreria requests...")\nres = requests.get("https://httpbin.org/get")\nprint("Codigo HTTP:", res.status_code)\nprint("Respuesta:", res.json())',
        4: '# Script continuo 24/7 en segundo plano\nimport time, datetime\n\nprint("Iniciando servicio de monitoreo 24/7...")\nfor i in range(1, 10):\n    ahora = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")\n    print(f"[{ahora}] Ciclo {i}: Servidor activo y respondiendo OK")\n    time.sleep(2)\nprint("Ciclo finalizado con exito")',
        5: '# Plantilla Bot de Discord 24/7 en PocketHost\n# La libreria discord.py se descarga automaticamente si no esta instalada\nimport sys, os, time\n\nTOKEN = "PEGA_AQUI_TU_TOKEN"\n\ntry:\n    import discord\n    from discord.ext import commands\n    print("[PocketHost] discord.py listo y cargado.")\nexcept ImportError:\n    print("[PocketHost] discord.py no detectado, descargando automaticamente via pip...")\n\nprint("[PocketHost 24/7] Preparando Bot de Discord...")\nif TOKEN == "PEGA_AQUI_TU_TOKEN":\n    print("[AVISO] Para conectar a Discord, genera un Token en https://discord.com/developers/applications y pegalo en la variable TOKEN.")\n    print("[PocketHost 24/7] Demostracion activa: El servicio en segundo plano sigue corriendo sin apagarse.")\nelse:\n    intents = discord.Intents.default()\n    intents.message_content = True\n    bot = commands.Bot(command_prefix="!", intents=intents)\n\n    @bot.event\n    async def on_ready():\n        print(f"[PocketHost 24/7] Bot en linea en Discord como: {bot.user}")\n\n    @bot.command()\n    async def ping(ctx):\n        await ctx.send("Pong! Servidor PocketHost respondiendo desde Android 24/7.")\n\n    bot.run(TOKEN)'
    },
    cpp: {
        1: '#include <iostream>\n\nint main() {\n    std::cout << "Hola desde C++ en PocketHost Server!" << std::endl;\n    std::cout << "Compilacion exitosa con GCC." << std::endl;\n    return 0;\n}',
        2: '#include <iostream>\n#include <vector>\n#include <algorithm>\n\nint main() {\n    std::vector<int> nums = {45, 12, 89, 23, 7, 56};\n    std::cout << "Original: ";\n    for(int x : nums) std::cout << x << " ";\n    std::cout << "\\n";\n\n    std::sort(nums.begin(), nums.end());\n    std::cout << "Ordenado: ";\n    for(int x : nums) std::cout << x << " ";\n    std::cout << "\\n";\n    return 0;\n}',
        3: '#include <iostream>\n#include <cmath>\n\nint main() {\n    std::cout << "Calculos matematicos con libreria cmath:\\n";\n    for(int i = 1; i <= 5; ++i) {\n        std::cout << "sqrt(" << i << ") = " << std::sqrt(i) << "\\n";\n    }\n    return 0;\n}',
        4: '#include <iostream>\n#include <chrono>\n#include <thread>\n\nint main() {\n    std::cout << "[PocketHost C++] Iniciando tarea continua..." << std::endl;\n    for(int i = 1; i <= 5; ++i) {\n        std::cout << "Paso " << i << " ejecutado correctamente." << std::endl;\n        std::this_thread::sleep_for(std::chrono::seconds(1));\n    }\n    std::cout << "[PocketHost C++] Tarea completada." << std::endl;\n    return 0;\n}',
        5: '#include <iostream>\n\nint main() {\n    std::cout << "[PocketHost] Bot Daemon en C++" << std::endl;\n    std::cout << "Servicio de fondo activo 24/7." << std::endl;\n    return 0;\n}'
    }
};

function setRunnerLang(lang) {
    currentLang = lang;
    document.getElementById('lang-btn-py').classList.toggle('active', lang === 'python');
    document.getElementById('lang-btn-cpp').classList.toggle('active', lang === 'cpp');
    document.getElementById('runner-filename').textContent = lang === 'python' ? 'script.py' : 'main.cpp';
    loadSnippet(1);
}

function loadSnippet(idx) {
    document.getElementById('code-editor').value = runnerSnippets[currentLang][idx] || '';
}

// Initial snippet
loadSnippet(1);

document.getElementById('code-editor')?.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        runCode();
    }
});

async function runCode() {
    const code = document.getElementById('code-editor').value;
    const stdin = document.getElementById('stdin-input').value;
    const autoLibs = document.getElementById('auto-install-checkbox').checked;

    const btn = document.getElementById('run-code-btn');
    const status = document.getElementById('runner-status');
    const stdout = document.getElementById('stdout-area');
    const stderr = document.getElementById('stderr-area');

    btn.disabled = true;
    btn.textContent = 'Ejecutando...';
    status.innerHTML = '<span style="color: var(--accent);">Procesando ejecucion...</span>';
    stdout.textContent = 'Enviando codigo al servidor...';
    stderr.textContent = '';

    try {
        const startTime = performance.now();
        const res = await fetch('/api/run', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                language: currentLang,
                code: code,
                stdin: stdin,
                auto_install_libs: autoLibs
            })
        });

        const data = await res.json();
        const duration = Math.round(performance.now() - startTime);

        let statusText = 'EXIT CODE ' + data.exit_code + ' (' + (data.duration_ms || duration) + 'ms)';
        if (data.installed_libs && data.installed_libs.length > 0) {
            statusText += ' | Librerias descargadas: ' + data.installed_libs.join(', ');
        }
        status.textContent = statusText;

        stdout.textContent = data.stdout || (data.exit_code === 0 ? 'Ejecucion completada sin salida.' : '');
        stderr.textContent = data.stderr || '';
    } catch (e) {
        status.textContent = 'Error de conexion';
        stdout.textContent = '';
        stderr.textContent = 'Fallo en la peticion al servidor: ' + e.message;
    } finally {
        btn.disabled = false;
        btn.textContent = 'Ejecutar Una Vez';
    }
}

async function saveCodeToFile() {
    const filename = currentLang === 'python' ? 'script.py' : 'main.cpp';
    const code = document.getElementById('code-editor').value;
    const file = new Blob([code], { type: 'text/plain' });
    try {
        const res = await fetch('/api/upload?name=' + filename, { method: 'POST', body: file });
        if (res.ok) {
            alert('Archivo guardado en el servidor como ' + filename);
        } else {
            alert('Error al guardar archivo');
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

// ---------------- 24/7 DAEMON CONTROLS ----------------
let daemonPollTimer = null;

async function checkDaemonStatus() {
    try {
        const res = await fetch('/api/daemon/status');
        const data = await res.json();
        const badge = document.getElementById('daemon-badge');
        const startBtn = document.getElementById('daemon-start-btn');
        const stopBtn = document.getElementById('daemon-stop-btn');

        if (!badge || !startBtn || !stopBtn) return;

        if (data.is_running) {
            badge.innerHTML = '<span style="color: var(--emerald); font-weight: bold;">ACTIVO (' + (data.uptime_formatted || '') + ') [' + (data.language || '') + ']</span>';
            startBtn.style.display = 'none';
            stopBtn.style.display = 'inline-block';

            if (data.logs && data.logs.length > 0) {
                const stdout = document.getElementById('stdout-area');
                if (stdout) {
                    stdout.textContent = data.logs.join('\n');
                }
            }
        } else {
            badge.innerHTML = '<span style="color: var(--text-muted);">Estado: Inactivo</span>';
            startBtn.style.display = 'inline-block';
            stopBtn.style.display = 'none';
        }
    } catch (e) {
        // Quiet error
    }
}

async function startDaemon() {
    const code = document.getElementById('code-editor').value;
    const autoLibs = document.getElementById('auto-install-checkbox').checked;
    const filename = currentLang === 'python' ? 'worker_24_7.py' : 'worker_24_7.cpp';

    const startBtn = document.getElementById('daemon-start-btn');
    startBtn.disabled = true;
    startBtn.textContent = 'Iniciando 24/7...';

    try {
        const res = await fetch('/api/daemon/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                code: code,
                language: currentLang,
                name: filename,
                auto_install_libs: autoLibs
            })
        });
        const data = await res.json();
        document.getElementById('runner-status').textContent = '24/7 INICIADO';
        checkDaemonStatus();
    } catch (e) {
        alert('Error al iniciar 24/7: ' + e.message);
    } finally {
        startBtn.disabled = false;
        startBtn.textContent = 'Ejecutar 24/7';
    }
}

async function stopDaemon() {
    const stopBtn = document.getElementById('daemon-stop-btn');
    stopBtn.disabled = true;
    stopBtn.textContent = 'Deteniendo...';

    try {
        const res = await fetch('/api/daemon/stop', { method: 'POST' });
        const data = await res.json();
        document.getElementById('runner-status').textContent = '24/7 DETENIDO';
        checkDaemonStatus();
    } catch (e) {
        alert('Error al detener 24/7: ' + e.message);
    } finally {
        stopBtn.disabled = false;
        stopBtn.textContent = 'Detener 24/7';
    }
}

// Poll daemon status every 3 seconds
if (!daemonPollTimer) {
    daemonPollTimer = setInterval(checkDaemonStatus, 3000);
}
checkDaemonStatus();

// Initial file load
loadFiles();
            """.trimIndent()
        )
    }
}
