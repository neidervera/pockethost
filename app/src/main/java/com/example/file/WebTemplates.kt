package com.example.file

import java.io.File

object WebTemplates {

    fun installWebFileManager(dir: File) {
        val filesHtml = File(dir, "files_web.html")
        filesHtml.writeText(
            """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Archivos - PocketHost Server</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .toolbar {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-bottom: 16px;
            align-items: center;
            justify-content: space-between;
        }
        .dropzone {
            border: 2px dashed var(--accent);
            background: rgba(0, 229, 255, 0.05);
            border-radius: 12px;
            padding: 24px;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s ease;
            margin-bottom: 18px;
        }
        .dropzone.dragover {
            background: rgba(0, 229, 255, 0.15);
            transform: scale(1.01);
        }
        .file-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
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
            border-radius: 14px;
            width: 100%;
            max-width: 760px;
            max-height: 85vh;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            box-shadow: 0 10px 40px rgba(0,0,0,0.7);
        }
        .modal-header {
            padding: 16px 20px;
            border-bottom: 1px solid var(--border-color);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .modal-body {
            padding: 20px;
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
            border-radius: 8px;
        }
        .modal-body pre {
            width: 100%;
            background: transparent;
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
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                <a href="index.html" class="btn" style="padding: 6px 14px; text-decoration: none;">Inicio</a>
                <span style="color: var(--emerald); font-weight: bold; font-size: 0.85rem;">SERVIDOR CONECTADO</span>
            </div>
            <h1>Gestion de Archivos</h1>
            <p class="subtitle">Subir, ver y descargar archivos alojados en el telefono</p>
        </header>

        <section class="card">
            <h2>Subir Archivos</h2>
            <div id="dropzone" class="dropzone">
                <p style="font-size: 1.05rem; font-weight: bold; margin-bottom: 6px; color: var(--accent);">
                    Haz clic para seleccionar archivos o arrastralos aqui
                </p>
                <p style="font-size: 0.85rem; color: var(--text-muted);">
                    Archivos HTML, CSS, JS, Python, C++, imagenes, videos, documentos
                </p>
                <input type="file" id="file-input" multiple style="display: none;">
            </div>
            <div id="upload-progress-container" style="display: none; margin-top: 10px;">
                <div style="background: #21262d; border-radius: 6px; overflow: hidden; height: 10px;">
                    <div id="upload-bar" style="background: var(--accent); width: 0%; height: 100%; transition: width 0.2s;"></div>
                </div>
                <p id="upload-status" style="font-size: 0.85rem; margin-top: 6px; color: var(--accent);"></p>
            </div>
        </section>

        <section class="card">
            <div class="toolbar">
                <h2>Lista de Archivos en Servidor</h2>
                <div style="display: flex; gap: 8px;">
                    <button onclick="createNewFolder()" class="btn">Nueva Carpeta</button>
                    <button onclick="loadFiles()" class="btn">Actualizar Lista</button>
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
    </div>

    <!-- Preview Modal -->
    <div id="preview-modal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h3 id="modal-title" style="color: #fff; font-size: 1.1rem;">Vista Previa</h3>
                <button onclick="closeModal()" class="btn" style="padding: 4px 10px;">Cerrar</button>
            </div>
            <div id="modal-body" class="modal-body"></div>
            <div style="padding: 12px 20px; border-top: 1px solid var(--border-color); display: flex; justify-content: flex-end; gap: 10px;">
                <a id="modal-download-btn" href="#" class="btn-action btn-download" download>Descargar Archivo</a>
                <button onclick="closeModal()" class="btn">Cerrar</button>
            </div>
        </div>
    </div>

    <script>
        let currentPath = '';

        async function loadFiles() {
            const tbody = document.getElementById('files-tbody');
            tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--text-muted);">Cargando...</td></tr>';
            try {
                const res = await fetch('/api/files?path=' + encodeURIComponent(currentPath));
                const data = await res.json();
                const files = data.files || [];

                if (files.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--text-muted); padding: 20px;">No hay archivos en el servidor. Usa la zona de arriba para subir uno.</td></tr>';
                    return;
                }

                let html = '';
                for (const f of files) {
                    const tag = f.is_dir ? '[CARPETA]' : '[' + (f.extension.toUpperCase() || 'FILE') + ']';
                    const date = new Date(f.modified).toLocaleString();
                    const viewBtn = !f.is_dir 
                        ? '<button onclick="previewFile(\'' + f.path + '\', \'' + f.mime + '\', \'' + f.name + '\')" class="btn-action btn-view">Ver</button>' 
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

        async function previewFile(filePath, mime, fileName) {
            const modal = document.getElementById('preview-modal');
            const title = document.getElementById('modal-title');
            const body = document.getElementById('modal-body');
            const dlBtn = document.getElementById('modal-download-btn');

            title.textContent = 'Archivo: ' + fileName;
            dlBtn.href = '/api/download?file=' + encodeURIComponent(filePath);
            dlBtn.setAttribute('download', fileName);
            body.innerHTML = '<p style="color: var(--accent);">Cargando vista previa...</p>';
            modal.style.display = 'flex';

            const previewUrl = '/api/preview?file=' + encodeURIComponent(filePath);

            if (mime.startsWith('image/')) {
                body.innerHTML = '<img src="' + previewUrl + '" alt="' + fileName + '">';
            } else if (mime.startsWith('video/')) {
                body.innerHTML = '<video controls autoplay src="' + previewUrl + '"></video>';
            } else if (mime.startsWith('audio/')) {
                body.innerHTML = '<audio controls autoplay src="' + previewUrl + '"></audio>';
            } else {
                try {
                    const res = await fetch(previewUrl);
                    const text = await res.text();
                    body.innerHTML = '<pre>' + escapeHtml(text.slice(0, 50000)) + '</pre>';
                } catch (e) {
                    body.innerHTML = '<p style="color: #ff3366;">No se pudo previsualizar el contenido del archivo.</p>';
                }
            }
        }

        function closeModal() {
            document.getElementById('preview-modal').style.display = 'none';
            document.getElementById('modal-body').innerHTML = '';
        }

        async function deleteFile(filePath, fileName) {
            if (!confirm('Deseas eliminar permanentemente "' + fileName + '" del servidor?')) return;
            try {
                const res = await fetch('/api/delete?file=' + encodeURIComponent(filePath), { method: 'POST' });
                const json = await res.json();
                if (json.success) {
                    loadFiles();
                } else {
                    alert('Error al eliminar el archivo.');
                }
            } catch (err) {
                alert('Error de red: ' + err.message);
            }
        }

        async function createNewFolder() {
            const name = prompt('Nombre de la nueva carpeta:');
            if (!name) return;
            try {
                const res = await fetch('/api/create_folder?name=' + encodeURIComponent(name) + '&dir=' + encodeURIComponent(currentPath), { method: 'POST' });
                const json = await res.json();
                if (json.success) {
                    loadFiles();
                } else {
                    alert('Error al crear carpeta');
                }
            } catch (err) {
                alert('Error: ' + err.message);
            }
        }

        const dropzone = document.getElementById('dropzone');
        const fileInput = document.getElementById('file-input');

        dropzone.addEventListener('click', () => fileInput.click());
        dropzone.addEventListener('dragover', (e) => { e.preventDefault(); dropzone.classList.add('dragover'); });
        dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
            if (e.dataTransfer.files.length) uploadFiles(e.dataTransfer.files);
        });
        fileInput.addEventListener('change', () => {
            if (fileInput.files.length) uploadFiles(fileInput.files);
        });

        async function uploadFiles(files) {
            const progressContainer = document.getElementById('upload-progress-container');
            const bar = document.getElementById('upload-bar');
            const status = document.getElementById('upload-status');

            progressContainer.style.display = 'block';

            for (let i = 0; i < files.length; i++) {
                const file = files[i];
                status.textContent = 'Subiendo (' + (i + 1) + '/' + files.length + '): ' + file.name + '...';
                bar.style.width = Math.round(((i) / files.length) * 100) + '%';

                try {
                    const uploadUrl = '/api/upload?name=' + encodeURIComponent(file.name) + '&dir=' + encodeURIComponent(currentPath);
                    const res = await fetch(uploadUrl, { method: 'POST', body: file });
                    if (!res.ok) throw new Error('HTTP ' + res.status);
                } catch (err) {
                    status.textContent = 'Error al subir ' + file.name + ': ' + err.message;
                    status.style.color = '#ff3366';
                    return;
                }
            }

            bar.style.width = '100%';
            status.textContent = 'Archivos subidos exitosamente al servidor';
            status.style.color = 'var(--emerald)';
            setTimeout(() => { progressContainer.style.display = 'none'; }, 2000);
            fileInput.value = '';
            loadFiles();
        }

        loadFiles();
    </script>
</body>
</html>
            """.trimIndent()
        )
    }

    fun installCodeRunnerWeb(dir: File) {
        val runnerHtml = File(dir, "runner.html")
        runnerHtml.writeText(
            """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ejecutor Python y C++ - PocketHost</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .lang-tabs {
            display: flex;
            gap: 10px;
            margin-bottom: 14px;
        }
        .lang-tab {
            padding: 10px 20px;
            border-radius: 8px;
            background: #161b22;
            border: 1px solid var(--border-color);
            color: var(--text-muted);
            cursor: pointer;
            font-weight: bold;
            font-size: 0.95rem;
            transition: all 0.2s;
        }
        .lang-tab.active {
            background: rgba(0, 229, 255, 0.15);
            border-color: var(--accent);
            color: var(--accent);
        }
        .editor-container {
            background: #06090e;
            border: 1px solid var(--border-color);
            border-radius: 10px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            margin-bottom: 16px;
        }
        .editor-bar {
            background: #0d1117;
            padding: 8px 14px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px solid #21262d;
            font-size: 0.85rem;
            color: var(--text-muted);
        }
        #code-editor {
            width: 100%;
            height: 320px;
            background: #06090e;
            color: #f0f6fc;
            font-family: 'Consolas', 'Courier New', monospace;
            font-size: 0.95rem;
            line-height: 1.5;
            padding: 14px;
            border: none;
            outline: none;
            resize: vertical;
            white-space: pre;
        }
        .options-row {
            display: flex;
            flex-wrap: wrap;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
            margin-bottom: 16px;
        }
        .checkbox-label {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            font-size: 0.9rem;
            color: var(--emerald);
            font-weight: 600;
            cursor: pointer;
        }
        .terminal {
            background: #05070a;
            border: 1px solid #21262d;
            border-radius: 10px;
            padding: 16px;
            font-family: monospace;
            font-size: 0.9rem;
            min-height: 180px;
            max-height: 420px;
            overflow-y: auto;
        }
        .term-header {
            display: flex;
            justify-content: space-between;
            color: var(--text-muted);
            font-size: 0.8rem;
            margin-bottom: 10px;
            border-bottom: 1px solid #1c222d;
            padding-bottom: 6px;
        }
        .stdout { color: #58a6ff; white-space: pre-wrap; word-break: break-all; }
        .stderr { color: #ff3366; white-space: pre-wrap; word-break: break-all; margin-top: 6px; }
        .meta-pill {
            display: inline-flex;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 0.75rem;
            font-weight: bold;
        }
        .pill-ok { background: rgba(0,230,118,0.2); color: var(--emerald); }
        .pill-err { background: rgba(255,51,102,0.2); color: #ff3366; }
    </style>
</head>
<body>
    <div class="glass-container">
        <header>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                <a href="index.html" class="btn" style="padding: 6px 14px; text-decoration: none;">Inicio</a>
                <span style="color: var(--emerald); font-weight: bold; font-size: 0.85rem;">SERVIDOR CONECTADO</span>
            </div>
            <h1>Ejecutor Python y C++</h1>
            <p class="subtitle">Ejecucion directa con descarga automatica de librerias faltantes</p>
        </header>

        <section class="card">
            <div class="lang-tabs">
                <button id="tab-python" class="lang-tab active" onclick="setLanguage('python')">
                    Python 3
                </button>
                <button id="tab-cpp" class="lang-tab" onclick="setLanguage('cpp')">
                    C++ (GCC / Clang)
                </button>
            </div>

            <div style="display: flex; gap: 8px; margin-bottom: 12px; align-items: center; flex-wrap: wrap;">
                <span style="font-size: 0.85rem; color: var(--text-muted);">Ejemplos:</span>
                <button onclick="loadTemplate(1)" class="btn" style="padding: 4px 10px; font-size: 0.8rem;">Basico</button>
                <button onclick="loadTemplate(2)" class="btn" style="padding: 4px 10px; font-size: 0.8rem;">Algoritmo / Calculo</button>
                <button id="btn-tmpl-lib" onclick="loadTemplate(3)" class="btn" style="padding: 4px 10px; font-size: 0.8rem;">Auto-descarga Libreria</button>
            </div>

            <div class="editor-container">
                <div class="editor-bar">
                    <span id="editor-filename">script.py</span>
                    <span>Atajo: Ctrl + Enter para ejecutar</span>
                </div>
                <textarea id="code-editor" spellcheck="false"></textarea>
            </div>

            <div style="margin-bottom: 14px;">
                <label style="font-size: 0.85rem; color: var(--text-muted); display: block; margin-bottom: 4px;">Entrada estandar (stdin) opcional:</label>
                <input type="text" id="stdin-input" placeholder="Datos de entrada..." style="width: 100%; background: #06090e; border: 1px solid var(--border-color); color: #fff; padding: 10px; border-radius: 8px; font-family: monospace;">
            </div>

            <div class="options-row">
                <label class="checkbox-label">
                    <input type="checkbox" id="auto-libs-check" checked>
                    <span>Descargar librerias automaticamente si faltan (pip / PyPI)</span>
                </label>

                <div style="display: flex; gap: 10px;">
                    <button onclick="saveToHosting()" class="btn">Guardar en Servidor</button>
                    <button id="run-btn" onclick="executeCode()" class="btn primary" style="font-size: 0.95rem; padding: 10px 22px;">
                        Ejecutar Codigo
                    </button>
                </div>
            </div>

            <div class="terminal">
                <div class="term-header">
                    <span>SALIDA DE CONSOLA</span>
                    <span id="term-status">Listo</span>
                </div>
                <div id="stdout" class="stdout">// Los resultados de la ejecucion apareceran aqui...</div>
                <div id="stderr" class="stderr"></div>
            </div>
        </section>
    </div>

    <script>
        let currentLang = 'python';

        const templates = {
            python: {
                1: '# Hola Mundo en Python\nimport sys, platform\nprint("Ejecutando Python en PocketHost Server!")\nprint("Version:", sys.version.split()[0])\nprint("Plataforma:", platform.system(), platform.machine())',
                2: '# Calculo de numeros primos\ndef es_primo(n):\n    if n < 2: return False\n    for i in range(2, int(n**0.5) + 1):\n        if n % i == 0: return False\n    return True\n\nprimos = [n for n in range(2, 100) if es_primo(n)]\nprint("Numeros primos menores a 100:")\nprint(primos)',
                3: '# Prueba de auto-descarga de libreria externa (requests)\n# PocketHost descargara la libreria faltante automaticamente\nimport requests\n\nprint("Consultando API con libreria requests...")\nres = requests.get("https://httpbin.org/get")\nprint("Codigo HTTP:", res.status_code)\nprint("Respuesta:", res.json())'
            },
            cpp: {
                1: '#include <iostream>\n\nint main() {\n    std::cout << "Hola desde C++ en PocketHost Server!" << std::endl;\n    std::cout << "Compilacion y ejecucion exitosa." << std::endl;\n    return 0;\n}',
                2: '#include <iostream>\n#include <vector>\n#include <algorithm>\n\nint main() {\n    std::vector<int> nums = {50, 20, 80, 10, 30, 90, 40};\n    std::sort(nums.begin(), nums.end());\n    std::cout << "Vector ordenado: ";\n    for(int x : nums) std::cout << x << " ";\n    std::cout << "\\n";\n    return 0;\n}',
                3: '#include <iostream>\n#include <cmath>\n\nint main() {\n    std::cout << "Calculos matematicos en C++:\\n";\n    for (int i = 1; i <= 5; ++i) {\n        std::cout << "sqrt(" << i << ") = " << std::sqrt(i) << "\\n";\n    }\n    return 0;\n}'
            }
        };

        function setLanguage(lang) {
            currentLang = lang;
            document.getElementById('tab-python').classList.toggle('active', lang === 'python');
            document.getElementById('tab-cpp').classList.toggle('active', lang === 'cpp');
            document.getElementById('editor-filename').textContent = lang === 'python' ? 'script.py' : 'main.cpp';
            loadTemplate(1);
        }

        function loadTemplate(idx) {
            document.getElementById('code-editor').value = templates[currentLang][idx] || '';
        }

        loadTemplate(1);

        document.getElementById('code-editor').addEventListener('keydown', (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                executeCode();
            }
        });

        async function executeCode() {
            const code = document.getElementById('code-editor').value;
            const stdin = document.getElementById('stdin-input').value;
            const autoLibs = document.getElementById('auto-libs-check').checked;

            const runBtn = document.getElementById('run-btn');
            const termStatus = document.getElementById('term-status');
            const stdoutEl = document.getElementById('stdout');
            const stderrEl = document.getElementById('stderr');

            runBtn.disabled = true;
            runBtn.textContent = 'Ejecutando...';
            termStatus.innerHTML = '<span style="color: var(--accent);">Procesando...</span>';
            stdoutEl.textContent = 'Enviando codigo al servidor...';
            stderrEl.textContent = '';

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

                let metaHtml = '';
                if (data.exit_code === 0) {
                    metaHtml += '<span class="meta-pill pill-ok">EXIT CODE 0 (OK)</span> ';
                } else {
                    metaHtml += '<span class="meta-pill pill-err">EXIT CODE ' + data.exit_code + '</span> ';
                }
                metaHtml += '<span style="color: var(--text-muted); font-size: 0.8rem;">' + (data.duration_ms || duration) + 'ms</span>';

                if (data.installed_libs && data.installed_libs.length > 0) {
                    metaHtml += ' <span class="meta-pill pill-ok">Librerias auto-descargadas: ' + data.installed_libs.join(', ') + '</span>';
                }

                termStatus.innerHTML = metaHtml;
                stdoutEl.textContent = data.stdout || (data.exit_code === 0 ? 'Ejecucion completada sin salida de texto.' : '');
                stderrEl.textContent = data.stderr || '';

            } catch (err) {
                termStatus.innerHTML = '<span class="meta-pill pill-err">ERROR</span>';
                stdoutEl.textContent = '';
                stderrEl.textContent = 'Error al comunicar con el servidor: ' + err.message;
            } finally {
                runBtn.disabled = false;
                runBtn.textContent = 'Ejecutar Codigo';
            }
        }

        async function saveToHosting() {
            const filename = currentLang === 'python' ? 'script.py' : 'main.cpp';
            const code = document.getElementById('code-editor').value;
            const file = new Blob([code], { type: 'text/plain' });
            try {
                const res = await fetch('/api/upload?name=' + filename, { method: 'POST', body: file });
                if (res.ok) {
                    alert('Archivo guardado en el servidor como ' + filename);
                } else {
                    alert('Error al guardar el archivo');
                }
            } catch (e) {
                alert('Error: ' + e.message);
            }
        }
    </script>
</body>
</html>
            """.trimIndent()
        )
    }
}
