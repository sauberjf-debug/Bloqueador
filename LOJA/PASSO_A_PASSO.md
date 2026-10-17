# Passo a passo: publicar na Google Play

## 1. Cadastrar a chave de assinatura no GitHub (uma vez só)
No repositório, vá em **Settings > Secrets and variables > Actions > New repository secret** e crie dois segredos:

| Nome | Valor |
| --- | --- |
| `KEYSTORE_BASE64` | todo o conteúdo do arquivo `keystore_base64.txt` (uma linha só) |
| `KEYSTORE_PASSWORD` | o conteúdo do arquivo `senha.txt` |

Crie também o segredo `ADMIN_PASSWORD` com a **senha de administrador que você escolher** (use 12 caracteres ou mais, com letras e números). Ela libera o APK de teste. Sem esse segredo, o APK sai **sem** senha.

> O APK de teste (link em Releases) só funciona depois de digitar essa senha em cada aparelho. O pacote `.aab` da Play Store sai **sem** senha, de propósito, para os testadores da loja.

Depois vá em **Actions > Gerar APK > Run workflow**. Ao terminar, a execução terá o arquivo `bloqueador-play-store-aab` em **Artifacts**.

> Guarde uma cópia de `upload-key.p12` e da senha em local seguro (nuvem pessoal, gerenciador de senhas). Nunca publique esses arquivos.

## 2. Criar a conta de desenvolvedor
Em https://play.google.com/console crie a conta e pague a taxa única (cerca de US$ 25). Será pedida a verificação de identidade.

Atenção: contas **pessoais** criadas após 13/11/2023 precisam fazer um **teste fechado com pelo menos 12 testadores, todos inscritos por 14 dias seguidos**, antes de pedir a publicação para todos. Contas de **organização** (empresa com número D-U-N-S) não têm essa exigência.

## 3. Criar o app
Play Console > **Criar app**: nome "Bloqueador de Ligações", idioma Português (Brasil), tipo App, gratuito. Preencha as seções de "Conteúdo do app" usando `LOJA/textos_da_loja.md`.

## 4. Enviar o pacote
**Testes > Teste fechado > Criar versão**, aceite o **Play App Signing** e envie o arquivo `app-release.aab`. Adicione os 12 testadores (por e-mail Google ou grupo) e envie o link de participação.

## 5. Depois dos 14 dias
**Painel > Solicitar acesso à produção**, responda o questionário e publique.

## Atualizações
Cada novo envio ao GitHub gera um `.aab` com número de versão maior automaticamente. Baixe de Actions > Artifacts e envie na Play Console.
