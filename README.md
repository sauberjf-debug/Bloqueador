# Bloqueador de Ligações (Android)

Rejeita automaticamente ligações de números que **não estão nos seus contatos**
(Claro, Vivo, TIM, qualquer operadora) antes mesmo de tocar.

## Como rodar
1. Instale o Android Studio e abra a pasta `BloqueadorLigacoes`.
2. Aguarde o *Gradle Sync* (ele baixa tudo e gera o wrapper sozinho).
3. Conecte o celular (depuração USB ligada) e clique em **Run**.
4. No app: toque em **1) Permitir contatos** e **2) Ativar como app de triagem**.

Requer Android 10 ou superior.

## Como funciona
- `ContactCallScreeningService`: o Android chama este serviço a cada ligação recebida.
  Ele consulta os contatos e, se o número não for conhecido, responde com *rejeitar*.
- `MainActivity`: pede a permissão de contatos, pede o papel de triagem de chamadas,
  tem os botões liga/desliga e mostra o histórico de bloqueios.
- Em caso de erro ou falta de permissão, a ligação **passa** (falha segura).
- Chamadas de emergência nunca são triadas pelo Android.

## Cuidados
- Ligações legítimas de números desconhecidos (entregador, banco, médico, escola)
  também serão bloqueadas. Use o botão **Bloqueio ativo** para desligar quando
  estiver esperando alguma ligação.
- Quem ligou cai na caixa postal ou ouve ocupado, conforme a operadora.
- Alguns fabricantes (Samsung, Xiaomi) têm filtros próprios que podem disputar o papel de triagem.

## Próximos passos possíveis
- Lista extra de números liberados (whitelist) e bloqueados (blacklist)
- Horários (ex.: só bloquear de madrugada)
- Prefixos de telemarketing (0303, 4004 etc.) em lista própria
- Versão com assinatura premium e publicação na Play Store
  (a permissão de contatos exige formulário de declaração e política de privacidade)
