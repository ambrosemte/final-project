$(document).ready(function () {
    const API_BASE_URL = '/api';

    // Camera functionality
    const video = document.getElementById('camera-viewfinder');
    const canvas = document.getElementById('captured-image');
    const ctx = canvas.getContext('2d');
    let currentStream = null;
    let currentMode = 'entry'; // 'entry' or 'exit'


    // Mode selection
    $('#entry-mode-btn').click(function () {
        currentMode = 'entry';
        $(this).addClass('btn-primary').removeClass('btn-outline-primary');
        $('#exit-mode-btn').addClass('btn-outline-danger').removeClass('btn-danger');
        updateModeUI();
    });

    $('#exit-mode-btn').click(function () {
        currentMode = 'exit';
        $(this).addClass('btn-danger').removeClass('btn-outline-danger');
        $('#entry-mode-btn').addClass('btn-outline-primary').removeClass('btn-primary');
        updateModeUI();
    });

    function updateModeUI() {
        if (cameraRunning) {
            startCamera(); // restart the stream in the new mode
        }
        if (currentMode === 'entry') {
            $('#reject-entry-btn').show();
            $('#reject-exit-btn').hide();
        } else {
            $('#reject-entry-btn').hide();
            $('#reject-exit-btn').show();
        }
    }

    // Camera functions
    let cameraRunning = false;
    updateCameraButtons();

    $('#start-btn').click(function () {
        startCamera();
    })

    function startCamera() {
        stopCamera();

        const FRAME_URL = `${API_BASE_URL}/vehicles/${currentMode}-scan`;
        video.src = `${FRAME_URL}?t=${new Date().getTime()}`;
        cameraRunning = true;
        updateCameraButtons();
    }

    $('#stop-btn').click(function () {
        stopCamera();
    })

    function stopCamera() {
        // Clearing the src closes the stream, which lets the server release the camera
        video.removeAttribute('src');
        cameraRunning = false;
        updateCameraButtons();
    }

    function updateCameraButtons() {
        $('#start-btn').prop('disabled', cameraRunning);
        $('#stop-btn').prop('disabled', !cameraRunning);
        $('#capture-btn').prop('disabled', !cameraRunning);
    }

    video.onerror = function () {
        if (cameraRunning) {
            stopCamera();
            showToast('Camera not available. Check camera.source and camera permissions.', 'danger');
        }
    };

    // Capture image: copy the frame currently shown in the viewfinder
    $('#capture-btn').click(function () {
        if (!cameraRunning || !video.naturalWidth) {
            showToast('No camera frame to capture yet', 'warning');
            return;
        }
        canvas.width = video.naturalWidth;
        canvas.height = video.naturalHeight;
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

        // Show the captured image
        canvas.style.display = 'block';
        $('#no-capture-message').hide();
        logActivity(`Image captured (${$('#detected-plate').text()})`);
    });

    function updateVehicleInfo() {
        $.get(`${API_BASE_URL}/vehicles/entry-last`)
            .done(function (data) {
                $('#detected-plate').text(data.plate || '---');
                $('#plate-confidence').text(data.confidence || '---');
            })
            .fail(function (err) {
                $('#detected-plate').text(err.responseText);
                $('#plate-confidence').text('Error');
            });
    }

    // Update every 200ms
    setInterval(updateVehicleInfo, 200);

    $('#reject-entry-btn').click(function () {
        const plate = $('#detected-plate').text();
        logActivity(`Entry for ${plate} was rejected at ${new Date().toLocaleTimeString()}`);

        $.post(`${API_BASE_URL}/entries/reject`, {
            licensePlate: plate,
            type: 'entry',
            timestamp: new Date().toISOString()
        }).then(function () {
            showToast('Entry rejected', 'warning');
        });
    });

    $('#reject-exit-btn').click(function () {
        const plate = $('#detected-plate').text();
        logActivity(`Exit for ${plate} was rejected at ${new Date().toLocaleTimeString()}`);

        $.post(`${API_BASE_URL}/entries/reject`, {
            licensePlate: plate,
            type: 'exit',
            timestamp: new Date().toISOString()
        }).then(function () {
            showToast('Exit rejected', 'warning');
        });
    });


    function logActivity(message) {
        const timestamp = new Date().toLocaleTimeString();
        $('#camera-activity-log').prepend(`
                    <div class="log-entry">
                        <small class="text-muted">${timestamp}</small>
                        <p class="mb-1">${message}</p>
                    </div>
                `);
    }

    // Clean up camera when leaving the page
    $(window).on('beforeunload', function () {
        stopCamera();
    });
});