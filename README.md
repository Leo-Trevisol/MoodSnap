<h1 align="center">🧠📅 MoodSnap</h1>

<p align="center">
  <strong>MoodSnap</strong> é um aplicativo Android desenvolvido em <strong>Kotlin</strong> para 
  <strong>controle diário de humor</strong>.  
  O app permite registrar emoções, acompanhar padrões ao longo do tempo e visualizar estatísticas
  através de gráficos interativos.
</p>

<p align="center">
  Projeto desenvolvido com foco em <strong>boas práticas</strong>, 
  <strong>arquitetura organizada</strong> e <strong>integração com Firebase</strong>, 
  sendo ideal para <strong>estudo</strong> e <strong>portfólio</strong>.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-Android-blueviolet?logo=kotlin" />
  <img src="https://img.shields.io/badge/Android-SDK-green?logo=android" />
  <img src="https://img.shields.io/badge/MVVM-Architecture-orange" />
  <img src="https://img.shields.io/badge/Room-Database-6db33f" />
  <img src="https://img.shields.io/badge/Firebase-Auth%20%26%20Firestore-ffca28?logo=firebase" />
  <img src="https://img.shields.io/badge/MPAndroidChart-Graphs-blue" />
  <img src="https://img.shields.io/badge/ViewBinding-UI-lightgrey" />
</p>

<hr/>

<section>
  <h2>✨ Funcionalidades</h2>
  <ul>
    <li>📆 Registro diário de humor</li>
    <li>🎭 Seleção de emoções com cores e ícones</li>
    <li>📝 Edição da descrição do dia</li>
    <li>📊 Dashboard com gráficos de barras e radar</li>
    <li>🔔 Notificações para lembrar de registrar o humor</li>
    <li>🌍 Suporte a múltiplos idiomas</li>
    <li>🎨 Customização de fontes</li>
    <li>🔐 Login com Google (Firebase Authentication)</li>
    <li>☁️ Sincronização com Firebase Firestore</li>
    <li>🚀 Onboarding interativo</li>
  </ul>
</section>

<hr/>

<section>
  <h2>🧱 Arquitetura do Projeto</h2>
  <p>
    O MoodSnap segue uma arquitetura baseada em <strong>MVVM (Model–View–ViewModel)</strong>,
    garantindo separação de responsabilidades, melhor testabilidade e manutenção.
  </p>

  <pre>
com.br.leo.moodsnap
│
├── service
│   └── model              (Modelos de dados)
│
├── repository
│   ├── dao                (DAOs - Room)
│   ├── database           (Banco de dados local)
│   └── MoodRepository
│
├── ui
│   ├── home               (Tela principal e calendário)
│   ├── dashboard          (Gráficos e estatísticas)
│   ├── dialog             (Dialogs e BottomSheets)
│   ├── edit               (Edição de registros)
│   ├── preview            (Preview de imagens)
│   └── notifications      (Notificações)
│
├── viewmodel              (ViewModels)
├── utils                  (Utilitários)
└── MainActivity
  </pre>
</section>

<hr/>

<section>
  <h2>🛠️ Tecnologias Utilizadas</h2>
  <ul>
    <li><strong>Kotlin</strong></li>
    <li><strong>Android SDK</strong></li>
    <li><strong>MVVM</strong></li>
    <li><strong>ViewBinding</strong></li>
    <li><strong>Room Database</strong></li>
    <li><strong>Firebase Authentication</strong></li>
    <li><strong>Firebase Firestore</strong></li>
    <li><strong>Google Sign-In</strong></li>
    <li><strong>MPAndroidChart</strong></li>
    <li><strong>Glide</strong></li>
    <li><strong>TapTargetView</strong></li>
  </ul>
</section>

<hr/>

<section>
  <h2>⚙️ Configurações do Projeto</h2>
  <ul>
    <li><strong>Min SDK:</strong> 24</li>
    <li><strong>Target SDK:</strong> 35</li>
    <li><strong>Compile SDK:</strong> 35</li>
    <li><strong>Java Version:</strong> 11</li>
  </ul>
</section>

<hr/>

<section>
  <h2>▶️ Como Executar o Projeto</h2>
  <ol>
    <li>Clone o repositório:
      <pre>git clone https://github.com/seu-usuario/moodsnap.git</pre>
    </li>
    <li>Abra o projeto no <strong>Android Studio</strong></li>
    <li>Crie um projeto no <strong>Firebase Console</strong></li>
    <li>Adicione o arquivo <code>google-services.json</code> em:
      <pre>app/google-services.json</pre>
    </li>
    <li>Sincronize o Gradle e execute 🚀</li>
  </ol>
</section>

<hr/>

<section>
  <h2>🔒 Firebase</h2>
  <p>
    Por motivos de segurança, o arquivo <code>google-services.json</code> 
    <strong>não é versionado</strong>.
  </p>
  <p>
    Cada desenvolvedor deve gerar seu próprio arquivo no Firebase Console.
  </p>
</section>

<hr/>

<section>
  <h2>🔐 Login com Google, SHA-1 e Ambientes (Homologação e Produção)</h2>

  <p>
    O login com Google utilizando o <strong>Firebase Authentication</strong> depende da
    identificação segura do aplicativo Android. Essa identificação é feita por meio do
    <strong>certificado de assinatura</strong> do app, representado pelo <strong>SHA-1</strong>.
  </p>

  <h3>📌 O que é o SHA-1?</h3>
  <p>
    O SHA-1 é um hash gerado a partir da <strong>chave de assinatura</strong> do aplicativo.
    Ele funciona como uma impressão digital do app, permitindo que o Firebase e os
    serviços do Google confirmem que as requisições de autenticação estão vindo
    de um aplicativo confiável.
  </p>

  <h3>⚙️ Como o login com Google funciona</h3>
  <ol>
    <li>O aplicativo solicita a autenticação do usuário via Google</li>
    <li>O Google valida a identidade do usuário</li>
    <li>O Firebase verifica se o <strong>SHA-1</strong> do app está cadastrado no projeto</li>
    <li>Se o SHA-1 for válido, o Firebase autoriza o login e retorna as credenciais</li>
  </ol>

  <p>
    Caso o SHA-1 não esteja cadastrado corretamente, o login com Google falha,
    mesmo que todas as dependências estejam configuradas corretamente.
  </p>

  <h3>🧪 Ambiente de Homologação (Debug)</h3>
  <p>
    No ambiente de homologação (debug), o aplicativo é assinado automaticamente
    pelo Android Studio com uma <strong>chave de debug</strong>.
  </p>

  <p>
    Para que o login com Google funcione nesse ambiente, é necessário:
  </p>

  <ul>
    <li>Executar o comando <code>./gradlew signingReport</code></li>
    <li>Copiar o <strong>SHA-1 da variante debug</strong></li>
    <li>Adicionar esse SHA-1 no Firebase Console</li>
    <li>Baixar e substituir o arquivo <code>google-services.json</code> no projeto</li>
  </ul>

  <p>
    Esse processo permite testar o login com Google localmente durante o desenvolvimento.
  </p>

  <h3>🚀 Ambiente de Produção (Release)</h3>
  <p>
    No ambiente de produção, o aplicativo é assinado com uma
    <strong>chave de release</strong>, que é diferente da chave de debug.
  </p>

  <p>
    Isso significa que:
  </p>

  <ul>
    <li>O SHA-1 da versão release é diferente do debug</li>
    <li>Esse SHA-1 também precisa ser cadastrado no Firebase</li>
    <li>O <code>google-services.json</code> deve estar atualizado com esse SHA-1</li>
  </ul>

  <p>
    Em aplicativos publicados na Google Play, geralmente é necessário cadastrar
    também o <strong>SHA-1 do App Signing</strong> fornecido pela Play Console.
  </p>

  <h3>⚠️ Observações Importantes</h3>
  <ul>
    <li>Cada ambiente possui seu próprio SHA-1</li>
    <li>Trocar de máquina, chave ou keystore exige nova configuração</li>
    <li>Sem o SHA-1 correto, o Google Sign-In não funciona</li>
    <li>O arquivo <code>google-services.json</code> nunca deve ser versionado</li>
  </ul>
</section>

<hr/>

<section>
  <h2>📷 Screenshots</h2>
  <p>
   (Futuros Screenshots)
  </p>
</section>

