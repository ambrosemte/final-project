$(document).ready(function () {
    const API_BASE_URL = '/api';

    // Modal instances
    const userModal = document.getElementById('userModal');
    const vehicleModal = document.getElementById('vehicleModal');
    const confirmModal = document.getElementById('confirmModal');

    // Current state
    let currentTab = 'home';
    let currentAction = '';
    let currentItemId = '';

    // Initialize the application
    init();

    function init() {
        setupEventListeners();
        loadHome();
    }

    function setupEventListeners() {
        // Tab navigation
        $('#home-tab').click(() => switchTab('home'));
        $('#vehicles-tab').click(() => switchTab('vehicles'));
        $('#users-tab').click(() => switchTab('users'));

        // Quick actions
        $('#quick-add-vehicle').click(() => openVehicleModal('add'));
        $('#quick-add-user').click(() => openUserModal('add'));

        // Main buttons
        $('#add-vehicle-btn').click(() => openVehicleModal('add'));
        $('#add-user-btn').click(() => openUserModal('add'));

        // Form submission
        // $('#save-vehicle-btn').click(saveVehicle);
        $('#save-user-btn').click(() => saveUser());

        // Refresh button
        $('#refresh-btn').click(refreshData);

        // Confirmation modal
        // $('#confirm-btn').click(confirmAction);
    }

    function switchTab(tab) {
        currentTab = tab;

        // Update active tab in sidebar
        $('.nav-link').removeClass('active');
        $(`#${tab}-tab`).addClass('active');

        // Update page title
        $('#page-title').text(
            tab === 'home' ? 'Dashboard' :
                tab === 'vehicles' ? 'Vehicle Management' : 'User Management'
        );

        // Hide all content sections
        $('[id$="-content"]').hide();

        // Show the selected content section
        $(`#${tab}-content`).show();

        // Load data for the selected tab
        if (tab === 'home') {
            loadHome();
        } else if (tab === 'vehicles') {
            loadVehicles();
        } else if (tab === 'users') {
            loadUsers();
        }
    }

    function refreshData() {
        if (currentTab === 'home') {
            loadHome();
        } else if (currentTab === 'vehicles') {
            loadVehicles();
        } else if (currentTab === 'users') {
            loadUsers();
        }
        showToast('Data refreshed successfully', 'success');
    }

    // Confirm Delete
    function confirmDelete(endpoint, id) {
        if (!confirm(`Are you sure you want to delete this ${endpoint}?`)) {
            return; // User clicked "Cancel" → stop execution
        }

        $.ajax({
            url: `${API_BASE_URL}/${endpoint}s/${id}`,
            type: 'DELETE'
        })
            .done(function (response) {
                showToast(`${endpoint} deleted successfully`, 'success');
                if (endpoint == 'user') {
                    loadUsers();
                } else {
                    loadVehicles();
                }
            })
            .fail(function (error) {
                showToast(`Error deleting ${endpoint}`, 'danger');
            });
    }

    // Home functions
    function loadHome() {
        $.when(
            $.get(`${API_BASE_URL}/vehicles`),
            $.get(`${API_BASE_URL}/users`),
        ).then(function (vehiclesResponse, usersResponse) {
            const vehicles = vehiclesResponse[0];
            const users = usersResponse[0];

            $('#total-vehicles').text(vehicles.length);
            $('#total-users').text(users.length);

            // Load recent activity (last 5 vehicles and users)
            const recentVehicles = vehicles.slice(-5).reverse();
            const recentUsers = users.slice(-5).reverse();

            let activityHtml = '<div class="list-group">';

            recentVehicles.forEach(vehicle => {
                activityHtml += `
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <h6 class="mb-1">New Vehicle Added</h6>
                                    <small>${new Date().toLocaleString()}</small>
                                </div>
                                <p class="mb-1">${vehicle.make} ${vehicle.model} (${vehicle.numberPlate})</p>
                            </div>
                        `;
            });

            recentUsers.forEach(user => {
                activityHtml += `
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <h6 class="mb-1">New User Registered</h6>
                                    <small>${new Date().toLocaleString()}</small>
                                </div>
                                <p class="mb-1">${user.name} (${user.email})</p>
                            </div>
                        `;
            });

            activityHtml += '</div>';
            $('#recent-activity').html(activityHtml);

        }).fail(function (error) {
            console.error('Error loading dashboard data:', error);
            showToast('Error loading dashboard data', 'danger');
        });
    }


    // User functions
    function loadUsers() {
        $.get(`${API_BASE_URL}/users`)
            .done(function (users) {
                const tableBody = $('#users-table-body');

                if (users.length === 0) {
                    tableBody.html('<tr><td colspan="6" class="text-center text-muted py-4">No users found</td></tr>');
                    return;
                }

                let html = '';
                users.forEach(user => {
                    const assignedVehicle = user.vehicle ?
                        `${user.vehicle.make} ${user.vehicle.model}` : 'Not assigned';

                    html += `
                                <tr>
                                    <td>${user.id}</td>
                                    <td>${user.name}</td>
                                    <td>${user.email}</td>
                                    <td><span class="badge ${user.role === 'admin' ? 'bg-danger' : 'bg-primary'}">${user.role}</span></td>
                                    <td>${assignedVehicle}</td>
                                    <td class="text-end table-actions">
                                        <button class="btn btn-sm btn-primary edit-user" data-id="${user.id}">
                                            <i class="fas fa-edit"></i> Edit
                                        </button>
                                        <button class="btn btn-sm btn-danger delete-user" data-id="${user.id}">
                                            <i class="fas fa-trash"></i> Delete
                                        </button>
                                    </td>
                                </tr>
                            `;
                });

                tableBody.html(html);

                // Add event listeners
                $('.edit-user').click(function () {
                    const id = $(this).data('id');
                    openUserModal('edit', id);
                });

                $('.delete-user').click(function () {
                    const id = $(this).data('id');
                    confirmDelete('user', id);
                });
            })
            .fail(function (error) {
                $('#users-table-body').html('<tr><td colspan="6" class="text-center text-danger py-4">Error loading users</td></tr>');
                console.error('Error loading users:', error);
                showToast('Error loading users', 'danger');
            });
    }

    function openUserModal(operation, id) {
        const title = userModal.querySelector(".modal-title");
        const form = userModal.querySelector("form");

        // Clear previous form data
        form.reset();

        // Adjust modal title and behavior based on operation
        switch (operation) {
            case "add":
                title.textContent = "Add New User";
                form.dataset.mode = "add";
                break;
            case "edit":
                title.textContent = "Edit User";
                form.dataset.mode = "edit";
                if (id) {
                    // Fetch existing user data (replace with real AJAX/fetch call)
                    fetch(`${API_BASE_URL}/users/${id}`)
                        .then(res => res.json())
                        .then(data => {
                            // Populate form with user data
                            form.querySelector("[name='name']").value = data.name || "";
                            form.querySelector("[name='email']").value = data.email || "";
                            //form.querySelector("[name='licensePlate']").value = data.licensePlate || "";
                        })
                        .catch(err => console.error("Failed to load user data:", err));
                }
                break;
            default:
                console.warn("Unknown operation:", operation);
        }

        // Finally, display the modal (using Bootstrap as example)
        if (typeof bootstrap !== "undefined") {
            const bsModal = new bootstrap.Modal(userModal);
            bsModal.show();
        } else {
            modal.style.display = "block"; // fallback if not using Bootstrap
        }
    }

    function saveUser() {
        const form = userModal.querySelector("form");
        const mode = form.dataset.mode; // "add" or "edit"
        const formData = new FormData(form);
        const jsonData = {};

        // Convert FormData → JSON object
        formData.forEach((value, key) => {
            jsonData[key] = value;
        });

        let url = `${API_BASE_URL}/users`;
        let method = "POST";

        if (mode === "edit") {
            const userId = form.dataset.id; // Store ID in dataset when editing
            url = `${API_BASE_URL}/users/${userId}`;
            method = "PUT";
        }

        fetch(url, {
            method: method,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(jsonData)
        })
            .then(res => {
                if (!res.ok) {
                    throw new Error(`Failed to save user: ${res.statusText}`);
                }
                return res.json();
            })
            .then(data => {
                showToast(data.message, 'success');
                if (typeof bootstrap !== "undefined") {
                    const bsModal = bootstrap.Modal.getInstance(userModal);
                    bsModal.hide();
                } else {
                    userModal.style.display = "none";
                }
                loadUsers();
            })
            .catch(err => {
                console.error(err);
                showToast('Error saving user. Please try again', 'error');
            });
    }


    // Vehicle functions
    function loadVehicles() {
        $.get(`${API_BASE_URL}/vehicles`)
            .done(function (vehicles) {
                const tableBody = $('#vehicles-table-body');

                if (vehicles.length === 0) {
                    tableBody.html('<tr><td colspan="7" class="text-center text-muted py-4">No vehicles found</td></tr>');
                    return;
                }

                let html = '';
                vehicles.forEach(vehicle => {
                    const assignedUser = vehicle.user ? vehicle.user.name : 'Not assigned';

                    html += `
                                <tr>
                                    <td>${vehicle.id}</td>
                                    <td>${vehicle.color}</td>
                                    <td>${vehicle.numberPlate}</td>
                                    <td>${assignedUser}</td>
                                    <td class="text-end table-actions">
                                        <button class="btn btn-sm btn-primary view-vehicle" data-id="${vehicle.id}">
                                            <i class="fas fa-view"></i> View
                                        </button>
                                        <button class="btn btn-sm btn-secondary edit-vehicle" data-id="${vehicle.id}">
                                            <i class="fas fa-edit"></i> Edit
                                        </button>
                                        <button class="btn btn-sm btn-danger delete-vehicle" data-id="${vehicle.id}">
                                            <i class="fas fa-trash"></i> Delete
                                        </button>
                                    </td>
                                </tr>
                            `;
                });

                tableBody.html(html);

                // Add event listeners
                $('.view-vehicle').click(function () {
                    const id = $(this).data('id');
                    openVehicleModal('view', id);
                });

                $('.edit-vehicle').click(function () {
                    const id = $(this).data('id');
                    openVehicleModal('edit', id);
                });

                $('.delete-vehicle').click(function () {
                    const id = $(this).data('id');
                    confirmDelete('vehicle', id);
                });
            })
            .fail(function (error) {
                $('#vehicles-table-body').html('<tr><td colspan="7" class="text-center text-danger py-4">Error loading vehicles</td></tr>');
                console.error('Error loading vehicles:', error);
                showToast('Error loading vehicles', 'danger');
            });
    }

    function openVehicleModal(operation, id) {
        const title = vehicleModal.querySelector(".modal-title");
        const form = vehicleModal.querySelector("form");

        // Clear previous form data
        form.reset();

        // Adjust modal title and behavior based on operation
        switch (operation) {
            case "add":
                title.textContent = "Add New Vehicle";
                form.dataset.mode = "add";
                break;
            case "edit":
                title.textContent = "Edit Vehicle";
                form.dataset.mode = "edit";
                if (id) {
                    // Fetch existing vehicle data (replace with real AJAX/fetch call)
                    fetch(`${API_BASE_URL}/vehicles/${id}`)
                        .then(res => res.json())
                        .then(data => {
                            // Populate form with vehicle data
                            form.querySelector("[name='name']").value = data.name || "";
                            form.querySelector("[name='color']").value = data.color || "";
                            form.querySelector("[name='numberPlate']").value = data.numberPlate || "";
                        })
                        .catch(err => console.error("Failed to load vehicle data:", err));
                }
                break;
            case "view":
                title.textContent = "View Vehicle";
                form.dataset.mode = "view";
                if (id) {
                    fetch(`${API_BASE_URL}/vehicles/${id}`)
                        .then(res => res.json())
                        .then(data => {
                            form.querySelector("[name='name']").value = data.name || "";
                            form.querySelector("[name='color']").value = data.color || "";
                            form.querySelector("[name='numberPlate']").value = data.numberPlate || "";
                            // Disable all inputs for read-only mode
                            form.querySelectorAll("input, select, textarea").forEach(el => el.disabled = true);
                            form.querySelectorAll("button").forEach(el => {
                                el.style.display = 'none';
                            });
                        })
                        .catch(err => console.error("Failed to load vehicle data:", err));
                }
                break;
            default:
                console.warn("Unknown operation:", operation);
        }

        // Finally, display the modal (using Bootstrap as example)
        if (typeof bootstrap !== "undefined") {
            const bsModal = new bootstrap.Modal(vehicleModal);
            bsModal.show();
        } else {
            modal.style.display = "block"; // fallback if not using Bootstrap
        }
    }


    window.showToast = showToast; // also used by camera-content.js
    function showToast(message, type) {
        // Create toast element
        const toast = $(`
                    <div class="position-fixed bottom-0 end-0 p-3" style="z-index: 11">
                        <div class="toast show align-items-center text-white bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                            <div class="d-flex">
                                <div class="toast-body">${message}</div>
                                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                            </div>
                        </div>
                    </div>
                `);

        $('body').append(toast);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            toast.remove();
        }, 5000);
    }
});
