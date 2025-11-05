// Управление чатом
class ChatManager {
    constructor(currentUser) {
        console.log(`[ChatManager] ===== INITIALIZING =====`);
        console.log(`[ChatManager] Current user:`, currentUser);

        this.currentUser = currentUser;
        this.messages = [];
        this.allUsers = [];
        this.selectedRecipient = null;
        this.pollingInterval = null;
        this.lastMessageTime = new Date(0);

        this.init();
    }

    init() {
        console.log(`[ChatManager] Setting up chat interface...`);

        // Отображаем имя пользователя
        const displayName = this.currentUser.displayName || this.currentUser.username;
        console.log(`[ChatManager] Display name: ${displayName}`);
        document.getElementById('current-user-name').textContent = displayName;

        // Обработка отправки сообщения
        document.getElementById('message-form').addEventListener('submit', (e) => {
            console.log(`[ChatManager] Message form submitted`);
            e.preventDefault();
            this.handleSendMessage();
        });

        // Обработка выхода
        document.getElementById('logout-btn').addEventListener('click', () => {
            console.log(`[ChatManager] Logout button clicked`);
            window.authManager.logout();
        });

        // Обработка обновления списка пользователей
        document.getElementById('refresh-users-btn').addEventListener('click', () => {
            console.log(`[ChatManager] Refresh users button clicked`);
            this.loadUsers();
        });

        // Загружаем список пользователей
        console.log(`[ChatManager] Loading users...`);
        this.loadUsers();

        // Загружаем существующие сообщения
        console.log(`[ChatManager] Loading existing messages...`);
        this.loadMessages();

        // Начинаем опрос новых сообщений
        console.log(`[ChatManager] Starting polling...`);
        this.startPolling();

        console.log(`[ChatManager] ===== INITIALIZATION COMPLETE =====`);
    }

    async loadUsers() {
        console.log(`[ChatManager] ===== LOADING USERS =====`);

        try {
            const users = await window.chatAPI.getAllUsers();
            console.log(`[ChatManager] Received ${users.length} users`);

            // Фильтруем текущего пользователя
            this.allUsers = users.filter(user => user.id !== this.currentUser.id);
            console.log(`[ChatManager] Filtered users (excluding self): ${this.allUsers.length}`);

            this.renderUsers();
        } catch (error) {
            console.error(`[ChatManager] Failed to load users:`, error);
        }

        console.log(`[ChatManager] ===== USERS LOADED =====`);
    }

    renderUsers() {
        console.log(`[ChatManager] Rendering ${this.allUsers.length} users...`);

        const usersList = document.getElementById('users-list');
        usersList.innerHTML = '';

        if (this.allUsers.length === 0) {
            usersList.innerHTML = '<div style="padding: 1rem; text-align: center; color: #9ca3af;">Нет других пользователей</div>';
            return;
        }

        this.allUsers.forEach(user => {
            const userItem = document.createElement('div');
            userItem.className = 'user-item';

            if (this.selectedRecipient && this.selectedRecipient.id === user.id) {
                userItem.classList.add('active');
            }

            // Первая буква имени для аватара
            const initial = (user.displayName || user.username).charAt(0).toUpperCase();

            userItem.innerHTML = `
                <div class="user-avatar">${initial}</div>
                <div class="user-details">
                    <div class="user-username">${user.username}</div>
                    ${user.displayName ? `<div class="user-display-name">${user.displayName}</div>` : ''}
                </div>
            `;

            userItem.addEventListener('click', () => {
                console.log(`[ChatManager] User selected:`, user);
                this.selectRecipient(user);
            });

            usersList.appendChild(userItem);
        });

        console.log(`[ChatManager] Users rendered`);
    }

    selectRecipient(user) {
        console.log(`[ChatManager] ===== SELECTING RECIPIENT =====`);
        console.log(`[ChatManager] Selected user:`, user);

        this.selectedRecipient = user;

        // Обновляем UI
        this.renderUsers();

        // Обновляем заголовок чата
        const recipientDiv = document.getElementById('chat-recipient');
        recipientDiv.innerHTML = `Чат с: <strong>${user.displayName || user.username}</strong>`;

        // Включаем форму отправки
        const input = document.getElementById('message-input');
        const button = document.querySelector('#message-form button');
        input.disabled = false;
        input.placeholder = 'Введите сообщение...';
        button.disabled = false;

        // Загружаем сообщения с этим пользователем
        this.renderMessagesForRecipient();

        console.log(`[ChatManager] ===== RECIPIENT SELECTED =====`);
    }

    async loadMessages() {
        console.log(`[ChatManager] ===== LOADING MESSAGES =====`);

        try {
            this.updateConnectionStatus(true);

            console.log(`[ChatManager] Fetching messages from API...`);
            const messages = await window.chatAPI.getAllMessages();

            console.log(`[ChatManager] Received ${messages.length} messages`);

            this.messages = messages;

            if (this.selectedRecipient) {
                this.renderMessagesForRecipient();
            }

            if (messages.length > 0) {
                const maxDate = Math.max(...messages.map(m => new Date(m.date)));
                this.lastMessageTime = new Date(maxDate);
                console.log(`[ChatManager] Last message time: ${this.lastMessageTime}`);
            } else {
                console.log(`[ChatManager] No messages found`);
            }

        } catch (error) {
            console.error(`[ChatManager] Failed to load messages:`, error);
            this.updateConnectionStatus(false);
        }

        console.log(`[ChatManager] ===== MESSAGES LOADED =====`);
    }

    renderMessagesForRecipient() {
        if (!this.selectedRecipient) {
            console.log(`[ChatManager] No recipient selected, showing placeholder`);
            return;
        }

        console.log(`[ChatManager] Rendering messages for recipient: ${this.selectedRecipient.username}`);

        const container = document.getElementById('messages');
        container.innerHTML = '';

        // Фильтруем сообщения: только между мной и выбранным получателем
        const conversationMessages = this.messages.filter(msg =>
            (msg.from === this.currentUser.id && msg.to === this.selectedRecipient.id) ||
            (msg.from === this.selectedRecipient.id && msg.to === this.currentUser.id)
        );

        console.log(`[ChatManager] Found ${conversationMessages.length} messages in conversation`);

        if (conversationMessages.length === 0) {
            container.innerHTML = '<div class="no-recipient-message">Начните общение, напишите первое сообщение!</div>';
            return;
        }

        conversationMessages.forEach((message, index) => {
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message';

            // Определяем, исходящее или входящее сообщение
            const isOutgoing = message.from === this.currentUser.id;

            if (isOutgoing) {
                messageDiv.classList.add('outgoing');
            } else {
                messageDiv.classList.add('incoming');
            }

            const date = new Date(message.date);
            const timeString = date.toLocaleTimeString('ru-RU', {
                hour: '2-digit',
                minute: '2-digit'
            });

            messageDiv.innerHTML = `
                <div class="message-header">${isOutgoing ? 'Вы' : this.selectedRecipient.displayName || this.selectedRecipient.username}</div>
                <div class="message-content">${this.escapeHtml(message.data)}</div>
                <div class="message-time">${timeString}</div>
            `;

            container.appendChild(messageDiv);
        });

        // Прокручиваем вниз
        container.scrollTop = container.scrollHeight;
        console.log(`[ChatManager] Messages rendered, scrolled to bottom`);
    }

    async handleSendMessage() {
        console.log(`[ChatManager] ===== SENDING MESSAGE =====`);

        if (!this.selectedRecipient) {
            console.warn(`[ChatManager] No recipient selected`);
            alert('Выберите получателя');
            return;
        }

        const input = document.getElementById('message-input');
        const messageText = input.value.trim();

        console.log(`[ChatManager] Message text: "${messageText}"`);
        console.log(`[ChatManager] Message length: ${messageText.length}`);
        console.log(`[ChatManager] Recipient:`, this.selectedRecipient);

        if (!messageText) {
            console.warn(`[ChatManager] Message is empty, not sending`);
            return;
        }

        try {
            console.log(`[ChatManager] Sending to API...`);
            console.log(`[ChatManager] From: ${this.currentUser.id}`);
            console.log(`[ChatManager] To: ${this.selectedRecipient.id}`);

            await window.chatAPI.sendMessage(
                this.currentUser.id,
                this.selectedRecipient.id,
                messageText
            );

            console.log(`[ChatManager] Message sent successfully`);

            input.value = '';
            console.log(`[ChatManager] Input cleared`);

            // Сразу обновляем сообщения
            console.log(`[ChatManager] Reloading messages...`);
            await this.loadMessages();

        } catch (error) {
            console.error(`[ChatManager] Failed to send message:`, error);
            alert('Не удалось отправить сообщение. Проверьте консоль для деталей.');
        }

        console.log(`[ChatManager] ===== MESSAGE SEND COMPLETED =====`);
    }

    startPolling() {
        console.log(`[ChatManager] Starting polling with interval: ${CONFIG.POLLING_INTERVAL}ms`);

        this.pollingInterval = setInterval(() => {
            this.pollNewMessages();
        }, CONFIG.POLLING_INTERVAL);

        console.log(`[ChatManager] Polling started`);
    }

    async pollNewMessages() {
        console.log(`[ChatManager] Polling for new messages...`);

        try {
            this.updateConnectionStatus(true);

            const messages = await window.chatAPI.getAllMessages();

            // Проверяем, есть ли новые сообщения
            const hasNewMessages = messages.some(msg => {
                const msgDate = new Date(msg.date);
                const isNew = msgDate > this.lastMessageTime;
                if (isNew) {
                    console.log(`[ChatManager] Found new message:`, msg);
                }
                return isNew;
            });

            if (hasNewMessages) {
                console.log(`[ChatManager] New messages detected, updating display`);
                this.messages = messages;

                if (this.selectedRecipient) {
                    this.renderMessagesForRecipient();
                }

                this.lastMessageTime = new Date(
                    Math.max(...messages.map(m => new Date(m.date)))
                );
            }

        } catch (error) {
            console.error(`[ChatManager] Polling error:`, error);
            this.updateConnectionStatus(false);
        }
    }

    stop() {
        console.log(`[ChatManager] Stopping polling...`);

        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
            console.log(`[ChatManager] Polling stopped`);
        } else {
            console.log(`[ChatManager] No polling interval to stop`);
        }
    }

    updateConnectionStatus(connected) {
        console.log(`[ChatManager] Connection status: ${connected ? 'connected' : 'disconnected'}`);

        const indicator = document.getElementById('connection-status');
        if (connected) {
            indicator.classList.remove('disconnected');
        } else {
            indicator.classList.add('disconnected');
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}
