document.addEventListener('DOMContentLoaded', () => {
    const headerCanvas = document.getElementById('header-canvas');
    if (!headerCanvas) return;

    const gl = headerCanvas.getContext('webgl2', { alpha: true, depth: false, stencil: false, antialias: false }) || headerCanvas.getContext('webgl', { alpha: true, depth: false, stencil: false, antialias: false });
    if (!gl) {
        console.warn("WebGL is not supported on this browser.");
        return;
    }
    gl.clearColor(0.0, 0.0, 0.0, 0.0);
    headerCanvas.width = headerCanvas.clientWidth;
    headerCanvas.height = headerCanvas.clientHeight;

    const config = { DENSITY_DISSIPATION: 0.97, VELOCITY_DISSIPATION: 0.98, PRESSURE_DISSIPATION: 0.8, PRESSURE_ITERATIONS: 20, CURL: 20, SPLAT_RADIUS: 0.005 };

    function getSupportedFormat(gl, internalFormat, format, type) {
        let texture = gl.createTexture();
        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.texImage2D(gl.TEXTURE_2D, 0, internalFormat, 4, 4, 0, format, type, null);
        let fbo = gl.createFramebuffer();
        gl.bindFramebuffer(gl.FRAMEBUFFER, fbo);
        gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, texture, 0);
        const status = gl.checkFramebufferStatus(gl.FRAMEBUFFER);
        gl.deleteTexture(texture);
        gl.deleteFramebuffer(fbo);
        return status === gl.FRAMEBUFFER_COMPLETE;
    }

    function getFormats(gl) {
        const isWebGL2 = gl.getParameter(gl.VERSION).includes('WebGL 2.0');
        const halfFloatExt = gl.getExtension('OES_texture_half_float');
        const halfFloatTexType = isWebGL2 ? gl.HALF_FLOAT : (halfFloatExt ? halfFloatExt.HALF_FLOAT_OES : null);
        let rgba, rg, r;
        if (isWebGL2 && halfFloatTexType) {
            if (getSupportedFormat(gl, gl.RGBA16F, gl.RGBA, halfFloatTexType)) rgba = { internalFormat: gl.RGBA16F, format: gl.RGBA, type: halfFloatTexType };
            if (getSupportedFormat(gl, gl.RG16F, gl.RG, halfFloatTexType)) rg = { internalFormat: gl.RG16F, format: gl.RG, type: halfFloatTexType };
            if (getSupportedFormat(gl, gl.R16F, gl.RED, halfFloatTexType)) r = { internalFormat: gl.R16F, format: gl.RED, type: halfFloatTexType };
        }
        const universalFallback = { internalFormat: gl.RGBA, format: gl.RGBA, type: gl.UNSIGNED_BYTE };
        return {
            rgba: rgba || universalFallback,
            rg: rg || universalFallback,
            r: r || universalFallback,
            supportLinearFiltering: gl.getExtension('OES_texture_float_linear')
        };
    }
    const formats = getFormats(gl);

    class GLProgram {
        constructor(vs, fs) { this.uniforms = {}; this.program = gl.createProgram(); gl.attachShader(this.program, vs); gl.attachShader(this.program, fs); gl.linkProgram(this.program); for (let i = 0; i < gl.getProgramParameter(this.program, gl.ACTIVE_UNIFORMS); i++) { let u = gl.getActiveUniform(this.program, i).name; this.uniforms[u] = gl.getUniformLocation(this.program, u); } }
        bind() { gl.useProgram(this.program); }
    }

    function compileShader(type, source) { const s = gl.createShader(type); gl.shaderSource(s, source); gl.compileShader(s); if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw gl.getShaderInfoLog(s); return s; }
    const baseVS = compileShader(gl.VERTEX_SHADER, `precision highp float; attribute vec2 aPosition; varying vec2 vUv; varying vec2 vL; varying vec2 vR; varying vec2 vT; varying vec2 vB; uniform vec2 texelSize; void main () { vUv = aPosition * 0.5 + 0.5; vL = vUv - vec2(texelSize.x, 0.0); vR = vUv + vec2(texelSize.x, 0.0); vT = vUv + vec2(0.0, texelSize.y); vB = vUv - vec2(0.0, texelSize.y); gl_Position = vec4(aPosition, 0.0, 1.0); }`);
    const clearFS = compileShader(gl.FRAGMENT_SHADER, `precision highp float; varying vec2 vUv; uniform sampler2D uTexture; uniform float value; void main () { gl_FragColor = value * texture2D(uTexture, vUv); }`);
    const displayFS = compileShader(gl.FRAGMENT_SHADER, `precision highp float; varying vec2 vUv; uniform sampler2D uTexture; void main () { gl_FragColor = texture2D(uTexture, vUv); }`);
    const splatFS = compileShader(gl.FRAGMENT_SHADER, `precision highp float; varying vec2 vUv; uniform sampler2D uTarget; uniform float aspectRatio; uniform vec3 color; uniform vec2 point; uniform float radius; void main () { vec2 p = vUv - point.xy; p.x *= aspectRatio; vec3 splat = exp(-dot(p, p) / radius) * color; vec3 base = texture2D(uTarget, vUv).xyz; gl_FragColor = vec4(base + splat, 1.0); }`);
    const advectionFS = compileShader(gl.FRAGMENT_SHADER, `precision highp float; varying vec2 vUv; uniform sampler2D uVelocity; uniform sampler2D uSource; uniform vec2 texelSize; uniform float dt; uniform float dissipation; void main () { vec2 coord = vUv - dt * texture2D(uVelocity, vUv).xy * texelSize; gl_FragColor = dissipation * texture2D(uSource, coord); }`);
    const divergenceFS = compileShader(gl.FRAGMENT_SHADER, `precision highp float; varying vec2 vUv; varying vec2 vL; varying vec2 vR; varying vec2 vT; varying vec2 vB; uniform sampler2D uVelocity; void main () { float L = texture2D(uVelocity, vL).x; float R = texture2D(uVelocity, vR).x; float T = texture2D(uVelocity, vT).y; float B = texture2D(uVelocity, vB).y; gl_FragColor = vec4(0.5 * (R - L + T - B), 0.0, 0.0, 1.0); }`);
    const curlFS = compileShader(gl.FRAGMENT_SHADER, `precision highp float; varying vec2 vUv; varying vec2 vL; varying vec2 vR; varying vec2 vT; varying vec2 vB; uniform sampler2D uVelocity; void main () { float L = texture2D(uVelocity, vL).y; float R = texture2D(uVelocity, vR).y; float T = texture2D(uVelocity, vT).x; float B = texture2D(uVelocity, vB).x; gl_FragColor = vec4(0.5 * (R - L - T + B), 0.0, 0.0, 1.0); }`);
    const vorticityFS = compileShader(gl.FRAGMENT_SHADER, `precision highp float; varying vec2 vUv; varying vec2 vL; varying vec2 vR; varying vec2 vT; varying vec2 vB; uniform sampler2D uVelocity; uniform sampler2D uCurl; uniform float curl; uniform float dt; void main () { float L = texture2D(uCurl, vL).x; float R = texture2D(uCurl, vR).x; float T = texture2D(uCurl, vT).x; float B = texture2D(uCurl, vB).x; float C = texture2D(uCurl, vUv).x; vec2 force = 0.5 * vec2(abs(T) - abs(B), abs(R) - abs(L)); force /= length(force) + 0.0001; force *= curl * C; vec2 velocity = texture2D(uVelocity, vUv).xy; gl_FragColor = vec4(velocity + force * dt, 0.0, 1.0); }`);
    const pressureFS = compileShader(gl.FRAGMENT_SHADER, `precision highp float; varying vec2 vUv; varying vec2 vL; varying vec2 vR; varying vec2 vT; varying vec2 vB; uniform sampler2D uPressure; uniform sampler2D uDivergence; void main () { float L = texture2D(uPressure, vL).x; float R = texture2D(uPressure, vR).x; float T = texture2D(uPressure, vT).x; float B = texture2D(uPressure, vB).x; float divergence = texture2D(uDivergence, vUv).x; float pressure = (L + R + B + T - divergence) * 0.25; gl_FragColor = vec4(pressure, 0.0, 0.0, 1.0); }`);
    const gradientSubtractFS = compileShader(gl.FRAGMENT_SHADER, `precision highp float; varying vec2 vUv; varying vec2 vL; varying vec2 vR; varying vec2 vT; varying vec2 vB; uniform sampler2D uPressure; uniform sampler2D uVelocity; void main () { float L = texture2D(uPressure, vL).x; float R = texture2D(uPressure, vR).x; float T = texture2D(uPressure, vT).x; float B = texture2D(uPressure, vB).x; vec2 velocity = texture2D(uVelocity, vUv).xy; velocity.xy -= 0.5 * vec2(R - L, T - B); gl_FragColor = vec4(velocity, 0.0, 1.0); }`);

    const programs = {
        clear: new GLProgram(baseVS, clearFS), display: new GLProgram(baseVS, displayFS), splat: new GLProgram(baseVS, splatFS), advection: new GLProgram(baseVS, advectionFS),
        divergence: new GLProgram(baseVS, divergenceFS), curl: new GLProgram(baseVS, curlFS), vorticity: new GLProgram(baseVS, vorticityFS), pressure: new GLProgram(baseVS, pressureFS), gradienSubtract: new GLProgram(baseVS, gradientSubtractFS)
    };

    function createFBO(w, h, internalFormat, format, type, param) {
        let fbo = gl.createFramebuffer(); gl.bindFramebuffer(gl.FRAMEBUFFER, fbo);
        let texture = gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, param); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, param);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.texImage2D(gl.TEXTURE_2D, 0, internalFormat, w, h, 0, format, type, null);
        gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, texture, 0);
        return { fbo, texture };
    }
    function createDoubleFBO(w, h, internalFormat, format, type, param) {
        let fbo1 = createFBO(w, h, internalFormat, format, type, param); let fbo2 = createFBO(w, h, internalFormat, format, type, param);
        return { get read() { return fbo1; }, get write() { return fbo2; }, swap() { let temp = fbo1; fbo1 = fbo2; fbo2 = temp; } }
    }

    const textureWidth = gl.drawingBufferWidth >> 1; const textureHeight = gl.drawingBufferHeight >> 1;
    const density = createDoubleFBO(textureWidth, textureHeight, formats.rgba.internalFormat, formats.rgba.format, formats.rgba.type, formats.supportLinearFiltering ? gl.LINEAR : gl.NEAREST);
    const velocity = createDoubleFBO(textureWidth, textureHeight, formats.rg.internalFormat, formats.rg.format, formats.rg.type, formats.supportLinearFiltering ? gl.LINEAR : gl.NEAREST);
    const divergence = createFBO(textureWidth, textureHeight, formats.r.internalFormat, formats.r.format, formats.r.type, gl.NEAREST);
    const curl = createFBO(textureWidth, textureHeight, formats.r.internalFormat, formats.r.format, formats.r.type, gl.NEAREST);
    const pressure = createDoubleFBO(textureWidth, textureHeight, formats.r.internalFormat, formats.r.format, formats.r.type, gl.NEAREST);
    
    const blit = (() => {
        gl.bindBuffer(gl.ARRAY_BUFFER, gl.createBuffer()); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, -1, 1, 1, 1, 1, -1]), gl.STATIC_DRAW);
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, gl.createBuffer()); gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new Uint16Array([0, 1, 2, 0, 2, 3]), gl.STATIC_DRAW);
        gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0); gl.enableVertexAttribArray(0);
        return (destination) => { gl.bindFramebuffer(gl.FRAMEBUFFER, destination); gl.drawElements(gl.TRIANGLES, 6, gl.UNSIGNED_SHORT, 0); }
    })();

    function splat(x, y, dx, dy, color) {
        programs.splat.bind();
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, velocity.read.texture);
        gl.uniform1i(programs.splat.uniforms.uTarget, 0);
        gl.uniform1f(programs.splat.uniforms.aspectRatio, gl.canvas.width / gl.canvas.height);
        gl.uniform2f(programs.splat.uniforms.point, x / gl.canvas.width, 1.0 - y / gl.canvas.height);
        gl.uniform3f(programs.splat.uniforms.color, dx, -dy, 1.0);
        gl.uniform1f(programs.splat.uniforms.radius, config.SPLAT_RADIUS);
        blit(velocity.write.fbo);
        velocity.swap();

        gl.bindTexture(gl.TEXTURE_2D, density.read.texture);
        gl.uniform1i(programs.splat.uniforms.uTarget, 0);
        gl.uniform3f(programs.splat.uniforms.color, color[0], color[1], color[2]);
        blit(density.write.fbo);
        density.swap();
    }

    let lastTime = Date.now();
    function update() {
        const dt = Math.min((Date.now() - lastTime) / 1000, 0.0166); lastTime = Date.now();
        gl.viewport(0, 0, textureWidth, textureHeight);
        
        const texelSize = { x: 1.0 / textureWidth, y: 1.0 / textureHeight };

        programs.advection.bind();
        gl.uniform2f(programs.advection.uniforms.texelSize, texelSize.x, texelSize.y);
        gl.uniform1f(programs.advection.uniforms.dt, dt);
        gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, velocity.read.texture); gl.uniform1i(programs.advection.uniforms.uVelocity, 0);
        gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, velocity.read.texture); gl.uniform1i(programs.advection.uniforms.uSource, 1);
        gl.uniform1f(programs.advection.uniforms.dissipation, config.VELOCITY_DISSIPATION);
        blit(velocity.write.fbo); velocity.swap();
        
        gl.bindTexture(gl.TEXTURE_2D, density.read.texture);
        gl.uniform1i(programs.advection.uniforms.uSource, 1);
        gl.uniform1f(programs.advection.uniforms.dissipation, config.DENSITY_DISSIPATION);
        blit(density.write.fbo); density.swap();
        
        programs.curl.bind();
        gl.uniform2f(programs.curl.uniforms.texelSize, texelSize.x, texelSize.y);
        gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, velocity.read.texture); gl.uniform1i(programs.curl.uniforms.uVelocity, 0);
        blit(curl.fbo);
        
        programs.vorticity.bind();
        gl.uniform2f(programs.vorticity.uniforms.texelSize, texelSize.x, texelSize.y);
        gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, velocity.read.texture); gl.uniform1i(programs.vorticity.uniforms.uVelocity, 0);
        gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, curl.texture); gl.uniform1i(programs.vorticity.uniforms.uCurl, 1);
        gl.uniform1f(programs.vorticity.uniforms.curl, config.CURL);
        gl.uniform1f(programs.vorticity.uniforms.dt, dt);
        blit(velocity.write.fbo); velocity.swap();
        
        programs.divergence.bind();
        gl.uniform2f(programs.divergence.uniforms.texelSize, texelSize.x, texelSize.y);
        gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, velocity.read.texture); gl.uniform1i(programs.divergence.uniforms.uVelocity, 0);
        blit(divergence.fbo);
        
        programs.clear.bind();
        gl.uniform1f(programs.clear.uniforms.value, config.PRESSURE_DISSIPATION);
        gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, pressure.read.texture); gl.uniform1i(programs.clear.uniforms.uTexture, 0);
        blit(pressure.write.fbo); pressure.swap();
        
        programs.pressure.bind();
        gl.uniform2f(programs.pressure.uniforms.texelSize, texelSize.x, texelSize.y);
        gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, divergence.texture); gl.uniform1i(programs.pressure.uniforms.uDivergence, 1);
        for (let i = 0; i < config.PRESSURE_ITERATIONS; i++) {
            gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, pressure.read.texture);
            gl.uniform1i(programs.pressure.uniforms.uPressure, 0);
            blit(pressure.write.fbo); pressure.swap();
        }
        
        programs.gradienSubtract.bind();
        gl.uniform2f(programs.gradienSubtract.uniforms.texelSize, texelSize.x, texelSize.y);
        gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, pressure.read.texture); gl.uniform1i(programs.gradienSubtract.uniforms.uPressure, 0);
        gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, velocity.read.texture); gl.uniform1i(programs.gradienSubtract.uniforms.uVelocity, 1);
        blit(velocity.write.fbo); velocity.swap();
        
        gl.viewport(0, 0, gl.drawingBufferWidth, gl.drawingBufferHeight);
        programs.display.bind();
        gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, density.read.texture); gl.uniform1i(programs.display.uniforms.uTexture, 0);
        blit(null);
        requestAnimationFrame(update);
    }
    
    function randomSplat() {
        if(document.hidden) return;
        const x = headerCanvas.width * Math.random();
        const y = headerCanvas.height * Math.random();
        const dx = (Math.random() - 0.5) * 1000;
        const dy = (Math.random() - 0.5) * 1000;
        const color = [1.0, 0.42, 0.29]; // #ff6c4a
        splat(x, y, dx, dy, color);
    }
    
    setInterval(randomSplat, 1500);
    update();
});