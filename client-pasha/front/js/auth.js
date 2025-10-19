// Управление авторизацией
class AuthManager {
    constructor() {
        console.log(`[AuthManager] Initializing...`);
        this.currentUser = null;
        this.init();
    }

    init() {
        console.log(`[AuthManager] Setting up event listeners`);

        // Переключение между формами
        document.querySelectorAll('.tab').forEach(tab => {
            tab.addEventListener('click', (e) => {
                const tabName = e.target.dataset.tab;
                console.log(`[AuthManager] Tab clicked: ${tabName}`);
                this.switchTab(tabName);
            });
        });

        // Обработка формы входа
        const loginForm = document.getElementById('login-form');
        console.log(`[AuthManager] Login form found:`, loginForm !== null);
        loginForm.addEventListener('submit', (e) => {
            console.log(`[AuthManager] Login form submitted`);
            e.preventDefault();
            this.handleLogin();
        });

        // Обработка формы регистрации
        const registerForm = document.getElementById('register-form');
        console.log(`[AuthManager] Register form found:`, registerForm !== null);
        registerForm.addEventListener('submit', (e) => {
            console.log(`[AuthManager] Register form submitted`);
            e.preventDefault();
            this.handleRegister();
        });

        // Проверка сохранённой сессии
        console.log(`[AuthManager] Checking for saved session...`);
        this.checkSession();
    }

    switchTab(tabName) {
        console.log(`[AuthManager] Switching to tab: ${tabName}`);

        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.auth-form').forEach(f => f.classList.remove('active'));

        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
        document.getElementById(`${tabName}-form`).classList.add('active');

        this.hideError();
    }

    async handleLogin() {
        console.log(`[AuthManager] ===== LOGIN ATTEMPT STARTED =====`);

        const usernameInput = document.getElementById('login-username');
        const username = usernameInput.value.trim();

        console.log(`[AuthManager] Username input value: "${username}"`);
        console.log(`[AuthManager] Username length: ${username.length}`);

        if (!username) {
            console.warn(`[AuthManager] Username is empty`);
            this.showError('Введите имя пользователя');
            return;
        }

        try {
            console.log(`[AuthManager] Fetching all users from API...`);
            const users = await window.chatAPI.getAllUsers();

            console.log(`[AuthManager] Received ${users.length} users`);
            console.log(`[AuthManager] Users list:`, users);

            console.log(`[AuthManager] Searching for user with username: "${username}"`);
            const user = users.find(u => {
                console.log(`[AuthManager] Comparing: "${u.username}" === "${username}" ?`, u.username === username);
                return u.username === username;
            });

            if (!user) {
                console.warn(`[AuthManager] User not found with username: "${username}"`);
                console.log(`[AuthManager] Available usernames:`, users.map(u => u.username));
                this.showError('Пользователь не найден. Пожалуйста, зарегистрируйтесь.');
                return;
            }

            console.log(`[AuthManager] User found:`, user);
            this.loginUser(user);

        } catch (error) {
            console.error(`[AuthManager] Login error:`, error);
            console.error(`[AuthManager] Error stack:`, error.stack);
            this.showError('Ошибка при входе. Проверьте консоль для деталей.');
        }

        console.log(`[AuthManager] ===== LOGIN ATTEMPT FINISHED =====`);
    }

    async handleRegister() {
        console.log(`[AuthManager] ===== REGISTRATION ATTEMPT STARTED =====`);

        const usernameInput = document.getElementById('register-username');
        const displayNameInput = document.getElementById('register-display-name');

        const username = usernameInput.value.trim();
        const displayName = displayNameInput.value.trim();

        console.log(`[AuthManager] Registration `);
        console.log(`[AuthManager] - username: "${username}" (length: ${username.length})`);
        console.log(`[AuthManager] - displayName: "${displayName}" (length: ${displayName.length})`);

        if (!username || !displayName) {
            console.warn(`[AuthManager] Missing required fields`);
            this.showError('Заполните все поля');
            return;
        }

        try {
            console.log(`[AuthManager] Calling createUser API...`);
            const createResult = await window.chatAPI.createUser(username, displayName);
            console.log(`[AuthManager] User creation result:`, createResult);

            console.log(`[AuthManager] Fetching user list to find created user...`);
            const users = await window.chatAPI.getAllUsers();
            console.log(`[AuthManager] Total users after creation: ${users.length}`);

            const user = users.find(u => u.username === username);

            if (user) {
                console.log(`[AuthManager] Created user found:`, user);
                this.loginUser(user);
            } else {
                console.error(`[AuthManager] Created user not found in user list!`);
                console.log(`[AuthManager] Available users:`, users);
                this.showError('Регистрация выполнена, но не удалось войти. Попробуйте войти вручную.');
            }

        } catch (error) {
            console.error(`[AuthManager] Registration error:`, error);
            console.error(`[AuthManager] Error type: ${error.name}`);
            console.error(`[AuthManager] Error message: ${error.message}`);
            console.error(`[AuthManager] Error stack:`, error.stack);

            if (error.message.includes('409') || error.message.includes('duplicate') || error.message.includes('exists')) {
                this.showError('Пользователь с таким именем уже существует.');
            } else if (error.message.includes('CORS') || error.message.includes('Failed to fetch')) {
                this.showError('Ошибка подключения к серверу. Проверьте CORS и доступность API.');
            } else {
                this.showError(`Ошибка регистрации: ${error.message}`);
            }
        }

        console.log(`[AuthManager] ===== REGISTRATION ATTEMPT FINISHED =====`);
    }

    loginUser(user) {
        console.log(`[AuthManager] ===== LOGGING IN USER =====`);
        console.log(`[AuthManager] User `, user);

        this.currentUser = user;

        console.log(`[AuthManager] Saving user to localStorage...`);
        localStorage.setItem('currentUser', JSON.stringify(user));
        console.log(`[AuthManager] User saved to localStorage`);

        console.log(`[AuthManager] Switching screens...`);
        document.getElementById('auth-screen').classList.remove('active');
        document.getElementById('chat-screen').classList.add('active');
        console.log(`[AuthManager] Screens switched`);

        // Инициализируем чат
        console.log(`[AuthManager] Initializing ChatManager...`);
        window.chatManager = new ChatManager(this.currentUser);
        console.log(`[AuthManager] ChatManager initialized`);

        console.log(`[AuthManager] ===== USER LOGGED IN SUCCESSFULLY =====`);
    }

    logout() {
        console.log(`[AuthManager] ===== LOGOUT STARTED =====`);

        this.currentUser = null;
        localStorage.removeItem('currentUser');
        console.log(`[AuthManager] User cleared from memory and localStorage`);

        document.getElementById('chat-screen').classList.remove('active');
        document.getElementById('auth-screen').classList.add('active');
        console.log(`[AuthManager] Screens switched back to auth`);

        // Останавливаем чат
        if (window.chatManager) {
            console.log(`[AuthManager] Stopping ChatManager...`);
            window.chatManager.stop();
            console.log(`[AuthManager] ChatManager stopped`);
        }

        console.log(`[AuthManager] ===== LOGOUT COMPLETED =====`);
    }

    checkSession() {
        console.log(`[AuthManager] Checking for existing session...`);

        const savedUser = localStorage.getItem('currentUser');
        console.log(`[AuthManager] Saved user `, savedUser);

        if (savedUser) {
            try {
                const user = JSON.parse(savedUser);
                console.log(`[AuthManager] Found saved session for user:`, user);
                this.loginUser(user);
            } catch (error) {
                console.error(`[AuthManager] Failed to parse saved user:`, error);
                localStorage.removeItem('currentUser');
            }
        } else {
            console.log(`[AuthManager] No saved session found`);
        }
    }

    showError(message) {
        console.log(`[AuthManager] Showing error: ${message}`);
        const errorDiv = document.getElementById('auth-error');
        errorDiv.textContent = message;
        errorDiv.classList.add('show');
    }

    hideError() {
        console.log(`[AuthManager] Hiding error`);
        const errorDiv = document.getElementById('auth-error');
        errorDiv.classList.remove('show');
    }
}

// Инициализация при загрузке
document.addEventListener('DOMContentLoaded', () => {
    console.log(`[Main] ===== APPLICATION STARTING =====`);
    console.log(`[Main] DOM Content Loaded`);
    console.log(`[Main] Config:`, CONFIG);

    console.log(`[Main] Initializing DistributedChatAPI...`);
    window.chatAPI = new DistributedChatAPI();
    console.log(`[Main] chatAPI initialized:`, window.chatAPI);

    console.log(`[Main] Initializing AuthManager...`);
    window.authManager = new AuthManager();
    console.log(`[Main] authManager initialized:`, window.authManager);

    console.log(`[Main] ===== APPLICATION STARTED =====`);
});
