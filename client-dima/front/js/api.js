// Класс для работы с API
class ChatAPI {
    constructor(baseURL) {
        this.baseURL = baseURL;
        console.log(`[ChatAPI] Initialized with baseURL: ${baseURL}`);
    }

    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;

        console.log(`[ChatAPI] Making request to: ${url}`);
        console.log(`[ChatAPI] Request options:`, options);

        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
            },
            ...options
        };

        try {
            console.log(`[ChatAPI] Sending fetch request...`);
            const response = await fetch(url, defaultOptions);

            console.log(`[ChatAPI] Response status: ${response.status} ${response.statusText}`);
            console.log(`[ChatAPI] Response headers:`, [...response.headers.entries()]);

            const contentType = response.headers.get('content-type');
            console.log(`[ChatAPI] Content-Type: ${contentType}`);

            let data;
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
                console.log(`[ChatAPI] Response `, data);
            } else {
                const text = await response.text();
                console.log(`[ChatAPI] Response text:`, text);
                data = { success: false, error: { message: text } };
            }

            if (!response.ok) {
                console.error(`[ChatAPI] Request failed with status ${response.status}`);
                console.error(`[ChatAPI] Error `, data);

                // Подробные логи для ошибок валидации
                if (data.error) {
                    console.error(`[ChatAPI] Error code: ${data.error.code}`);
                    console.error(`[ChatAPI] Error message: ${data.error.message}`);
                }

                throw new Error(data.error?.message || `Request failed with status ${response.status}`);
            }

            console.log(`[ChatAPI] Request successful`);
            return data;
        } catch (error) {
            console.error(`[ChatAPI] Exception during request to ${url}:`, error);
            console.error(`[ChatAPI] Error name: ${error.name}`);
            console.error(`[ChatAPI] Error message: ${error.message}`);
            console.error(`[ChatAPI] Error stack:`, error.stack);
            throw error;
        }
    }


    // Методы для работы с пользователями
    async createUser(username, displayName) {
        console.log(`[ChatAPI] Creating user: username=${username}, displayName=${displayName}`);

        const payload = {
            username: username,
            displayName: displayName
        };

        console.log(`[ChatAPI] User creation payload:`, payload);

        try {
            const result = await this.request('/users', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            console.log(`[ChatAPI] User created successfully:`, result);
            return result;
        } catch (error) {
            console.error(`[ChatAPI] Failed to create user:`, error);
            throw error;
        }
    }

    async getAllUsers() {
        console.log(`[ChatAPI] Fetching all users`);

        try {
            const result = await this.request('/users');
            console.log(`[ChatAPI] Fetched ${result.data?.length || 0} users`);
            return result;
        } catch (error) {
            console.error(`[ChatAPI] Failed to fetch users:`, error);
            throw error;
        }
    }

    async getUserById(userId) {
        console.log(`[ChatAPI] Fetching user by ID: ${userId}`);
        return this.request(`/users/${userId}`);
    }

    // Методы для работы с сообщениями
    // Методы для работы с сообщениями
    async sendMessage(fromUserId, toUserId, messageText) {
        console.log(`[ChatAPI] ===== SENDING MESSAGE =====`);
        console.log(`[ChatAPI] fromUserId: ${fromUserId} (type: ${typeof fromUserId})`);
        console.log(`[ChatAPI] toUserId: ${toUserId} (type: ${typeof toUserId})`);
        console.log(`[ChatAPI] messageText: "${messageText}" (type: ${typeof messageText})`);
        console.log(`[ChatAPI] messageText length: ${messageText?.length}`);

        const payload = {
            date: new Date().toISOString(),
            from: fromUserId,
            to: toUserId,
            data: messageText,
            type: 'STRING'
        };

        console.log(`[ChatAPI] Full payload:`, payload);
        console.log(`[ChatAPI] Payload as JSON:`, JSON.stringify(payload, null, 2));

        try {
            const result = await this.request('/messages', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            console.log(`[ChatAPI] Message sent successfully:`, result);
            return result;
        } catch (error) {
            console.error(`[ChatAPI] Failed to send message:`, error);
            throw error;
        }
    }


    async getAllMessages() {
        console.log(`[ChatAPI] Fetching all messages`);
        return this.request('/messages');
    }

    async getMessagesByUser(userId, direction = 'from') {
        console.log(`[ChatAPI] Fetching messages: ${direction}=${userId}`);
        return this.request(`/messages?${direction}=${userId}`);
    }

    async getConversation(user1Id, user2Id) {
        console.log(`[ChatAPI] Fetching conversation between ${user1Id} and ${user2Id}`);
        return this.request(`/messages/conversation?user1=${user1Id}&user2=${user2Id}`);
    }
}

// Класс для управления двумя бэкендами
class DistributedChatAPI {
    constructor() {
        console.log(`[DistributedChatAPI] Initializing...`);
        console.log(`[DistributedChatAPI] Local API: ${CONFIG.LOCAL_API}`);
        console.log(`[DistributedChatAPI] Remote API: ${CONFIG.REMOTE_API}`);

        this.localAPI = new ChatAPI(CONFIG.LOCAL_API);
        this.remoteAPI = new ChatAPI(CONFIG.REMOTE_API);

        console.log(`[DistributedChatAPI] Initialization complete`);
    }

    // Отправить сообщение на оба бэкенда
    async sendMessage(fromUserId, toUserId, messageText) {
        console.log(`[DistributedChatAPI] Sending message to both backends`);

        const promises = [
            this.localAPI.sendMessage(fromUserId, toUserId, messageText).catch(err => {
                console.error(`[DistributedChatAPI] Local API failed:`, err);
                return { error: err };
            }),
            this.remoteAPI.sendMessage(fromUserId, toUserId, messageText).catch(err => {
                console.error(`[DistributedChatAPI] Remote API failed:`, err);
                return { error: err };
            })
        ];

        try {
            const results = await Promise.all(promises);
            console.log(`[DistributedChatAPI] Both backends responded:`, results);

            const hasSuccess = results.some(r => !r.error);
            if (hasSuccess) {
                console.log(`[DistributedChatAPI] At least one backend succeeded`);
                return { success: true };
            } else {
                console.error(`[DistributedChatAPI] Both backends failed`);
                throw new Error('Both backends failed to send message');
            }
        } catch (error) {
            console.error(`[DistributedChatAPI] Error sending message:`, error);
            throw error;
        }
    }

    // Получить все сообщения с обоих бэкендов
    async getAllMessages() {
        console.log(`[DistributedChatAPI] Fetching messages from both backends`);

        try {
            const localResponse = await this.localAPI.getAllMessages();
            console.log(`[DistributedChatAPI] Local messages:`, localResponse);

            const remoteResponse = await this.remoteAPI.getAllMessages().catch(err => {
                console.warn(`[DistributedChatAPI] Remote API unavailable:`, err);
                return new [];  // ИСПРАВЛЕНО: должно быть {  [] }
            });
            console.log(`[DistributedChatAPI] Remote messages:`, remoteResponse);

            // Объединяем и дедуплицируем сообщения по ID
            const allMessages = [
                ...(localResponse.data || []),
                ...(remoteResponse.data || [])
            ];

            console.log(`[DistributedChatAPI] Total messages before deduplication: ${allMessages.length}`);

            // Убираем дубликаты по ID
            const uniqueMessages = Array.from(
                new Map(allMessages.map(msg => [msg.id, msg])).values()
            );

            console.log(`[DistributedChatAPI] Unique messages after deduplication: ${uniqueMessages.length}`);

            // Сортируем по дате
            const sorted = uniqueMessages.sort((a, b) =>
                new Date(a.date) - new Date(b.date)
            );

            return sorted;
        } catch (error) {
            console.error(`[DistributedChatAPI] Failed to fetch messages:`, error);
            // Возвращаем сообщения хотя бы с одного бэкенда
            try {
                const localResponse = await this.localAPI.getAllMessages();
                console.log(`[DistributedChatAPI] Falling back to local messages only`);
                return localResponse.data || [];
            } catch {
                console.error(`[DistributedChatAPI] Both backends failed`);
                return [];
            }
        }
    }


    // Создать пользователя на обоих бэкендах
    async createUser(username, displayName) {
        console.log(`[DistributedChatAPI] Creating user on both backends`);

        try {
            console.log(`[DistributedChatAPI] Creating on local backend...`);
            const localResponse = await this.localAPI.createUser(username, displayName);
            console.log(`[DistributedChatAPI] Local creation successful:`, localResponse);

            try {
                console.log(`[DistributedChatAPI] Creating on remote backend...`);
                const remoteResponse = await this.remoteAPI.createUser(username, displayName);
                console.log(`[DistributedChatAPI] Remote creation successful:`, remoteResponse);
            } catch (remoteError) {
                console.warn(`[DistributedChatAPI] Remote creation failed (non-critical):`, remoteError);
            }

            return localResponse;
        } catch (error) {
            console.error(`[DistributedChatAPI] Failed to create user:`, error);
            throw error;
        }
    }

    // Получить всех пользователей
    async getAllUsers() {
        console.log(`[DistributedChatAPI] Fetching all users`);

        try {
            const localResponse = await this.localAPI.getAllUsers();
            console.log(`[DistributedChatAPI] Users fetched:`, localResponse);
            return localResponse.data || [];
        } catch (error) {
            console.error(`[DistributedChatAPI] Failed to fetch users:`, error);
            return [];
        }
    }
}
