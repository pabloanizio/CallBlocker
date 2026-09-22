# 📞 CallBlocker

**CallBlocker** é um aplicativo Android moderno para bloqueio e gerenciamento de chamadas indesejadas de números fora da sua agenda de contatos, desenvolvido com **Jetpack Compose**, **Kotlin Coroutines/Flow**, **Room Database** e a API nativa **CallScreeningService** do Android.

---

## 🚀 Funcionalidades

- 🚫 **Bloqueio de Números Desconhecidos**: Intercepta automaticamente chamadas de números que não estão salvos nos seus contatos.
- 📇 **Verificação Inteligente de Contatos**: Valida contatos nativamente e através de busca por sufixo na agenda (`ContactsContract.PhoneLookup`).
- 🚨 **Modo de Emergência (Repetição de Chamadas)**: Permite que chamadas de emergência passem se o mesmo número ligar repetidamente dentro de um intervalo de tempo configurável (ex: 2 tentativas em 3 minutos).
- ⚙️ **Ações de Bloqueio Flexíveis**:
  - Rejeitar chamada imediatamente (sinal de ocupado).
  - Silenciar o toque da chamada.
  - Ocultar/Exibir chamadas bloqueadas no histórico nativo do discador.
- 📊 **Histórico Local de Chamadas**: Registra todas as chamadas bloqueadas em um banco de dados local (Room SQLite) com data, hora e ação tomada, além de permitir a limpeza do histórico.
- 🎛️ **Painel de Configurações**: Interface intuitiva para ajustar todas as regras de bloqueio e verificar permissões do sistema.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem**: Kotlin
- **Interface Gráfica**: Jetpack Compose + Material Design 3
- **Integração com Sistema**: `CallScreeningService` + `RoleManager` (Android 10+)
- **Banco de Dados Local**: Room Database + KSP
- **Armazenamento de Preferências**: Jetpack DataStore Preferences
- **Programação Assíncrona**: Kotlin Coroutines & Flow

---

## 🔑 Permissões Necessárias

Para o funcionamento correto do aplicativo, são solicitadas as seguintes permissões no primeiro uso:

1. **Contatos (`READ_CONTACTS`)**: Para verificar se o número que está ligando pertence à sua agenda de contatos.
2. **Filtrador Padrão de Chamadas (`RoleManager.ROLE_CALL_SCREENING`)**: Permissão nativa do Android para interceptar e gerenciar chamadas em tempo real.

---

## 📱 Estrutura do App

O aplicativo conta com duas abas principais:
1. **Histórico**: Exibe o log completo das chamadas bloqueadas.
2. **Configurações**: Status das permissões, toggles das regras de bloqueio e controles interativos do Modo de Emergência.

---

## 📦 Como Executar o Projeto

1. Clone o repositório:
   ```bash
   git clone https://github.com/pabloanizio/CallBlocker.git
   ```
2. Abra o projeto no **Android Studio**.
3. Aguarde o Gradle sincronizar as dependências.
4. Execute o app em um dispositivo físico ou emulador com Android 10 (API 29) ou superior.
