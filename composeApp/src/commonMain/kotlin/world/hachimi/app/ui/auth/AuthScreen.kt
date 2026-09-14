package world.hachimi.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import hachimiworld.composeapp.generated.resources.Res
import hachimiworld.composeapp.generated.resources.ms_arrow_back
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import world.hachimi.app.model.AuthViewModel
import world.hachimi.app.model.GlobalStore
import world.hachimi.app.ui.auth.components.CaptchaDialog
import world.hachimi.app.ui.auth.components.FormCard
import world.hachimi.app.ui.insets.currentSafeAreaInsets
import world.hachimi.app.util.singleLined

@Composable
fun AuthScreen(
    displayLoginAsInitial: Boolean,
    vm: AuthViewModel = koinViewModel()
) {
    DisposableEffect(vm) {
        vm.mounted()
        onDispose { vm.unmount() }
    }
    val global = koinInject<GlobalStore>()
    var isLogin by remember(displayLoginAsInitial) { mutableStateOf(displayLoginAsInitial) }

    Box(Modifier.fillMaxSize().padding(top = currentSafeAreaInsets().top)) {
        IconButton(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp).align(Alignment.TopStart),
            onClick = { global.nav.back() }) {
            Icon(vectorResource(Res.drawable.ms_arrow_back), contentDescription = "Back")
        }
        FormCard(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .widthIn(max = 512.dp)
                .fillMaxWidth()
                .align(Alignment.Center),
            title = {
                Text(
                    text = when {
                        isLogin -> "欢迎回家"
                        vm.regStep == 0 -> "成为神人"
                        vm.regStep == 1 -> "离神人很近了"
                        vm.regStep == 2 -> "只差一步"
                        else -> error("unreachable")
                    }
                )
            },
            subtitle = {
                if (!isLogin && vm.regStep != 0) Text(
                    text = when {
                        vm.regStep == 1 -> "已将验证码发送至邮箱，如未收到请检查垃圾箱"
                        vm.regStep == 2 -> "完善你的资料"
                        else -> error("unreachable")
                    }
                )
            }
        ) {
            if (isLogin) {
                LoginContent(vm, { isLogin = false })
            } else {
                RegisterContent(vm, { isLogin = true })
            }
        }

        if (vm.showCaptchaDialog) CaptchaDialog(onConfirm = {
            vm.finishCaptcha()
        })
    }
}

@Composable
private fun LoginContent(vm: AuthViewModel, toRegister: () -> Unit) {
    Column(Modifier) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = vm.email,
            onValueChange = { vm.email = it.singleLined() },
            label = { Text("邮箱") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(24.dp))
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = vm.password,
            onValueChange = { vm.password = it.singleLined() },
            label = { Text("密码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (!vm.isOperating && vm.email.isNotBlank() && vm.password.isNotBlank()) {
                    vm.startLogin()
                }
            })
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            modifier = Modifier.align(Alignment.End),
            onClick = { vm.forgetPassword() }
        ) {
            Text("忘记密码？")
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth()) {
            TextButton(modifier = Modifier, onClick = toRegister) {
                Text("创建账号")
            }

            Spacer(Modifier.weight(1f))

            Button(
                modifier = Modifier.width(110.dp),
                onClick = { vm.startLogin() },
                enabled = !vm.isOperating && vm.email.isNotBlank() && vm.password.isNotBlank(),
            ) {
                Text("登录")
            }
        }
    }
}

@Composable
private fun RegisterContent(vm: AuthViewModel, toLogin: () -> Unit) {
    Column(Modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (vm.regStep == 0) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = vm.regEmail,
                onValueChange = { vm.regEmail = it.singleLined() },
                label = { Text("邮箱") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = vm.regPassword,
                onValueChange = { vm.regPassword = it.singleLined() },
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = vm.regPasswordRepeat,
                onValueChange = { vm.regPasswordRepeat = it.singleLined() },
                label = { Text("确认密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )

            val enabled by remember {
                derivedStateOf {
                    vm.regEmail.isNotBlank() && vm.regPassword.isNotBlank()
                            && vm.regPassword == vm.regPasswordRepeat
                }
            }

            Row(Modifier.fillMaxWidth()) {
                TextButton(modifier = Modifier, onClick = toLogin) {
                    Text("登录")
                }

                Spacer(Modifier.weight(1f))

                Button(
                    modifier = Modifier.width(110.dp),
                    onClick = { vm.regNextStep() },
                    enabled = enabled && !vm.isOperating,
                ) {
                    Text("下一步")
                }
            }
        } else if (vm.regStep == 1) Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = vm.regCode,
                    onValueChange = { vm.regCode = it.singleLined() },
                    label = { Text("验证码") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (!vm.isOperating && vm.regCode.isNotBlank()) {
                            vm.regNextStep()
                        }
                    })
                )
            }
            Spacer(Modifier.height(8.dp))
            val sendCodeEnabled = vm.regCodeRemainSecs < 0
            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = { vm.regSendEmailCode() },
                enabled = sendCodeEnabled && !vm.isOperating
            ) {
                Text(
                    if (sendCodeEnabled) "重新发送"
                    else "重新发送 (${vm.regCodeRemainSecs} 秒)"
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { vm.regStep = 0 }) {
                    Text("上一步")
                }

                Spacer(Modifier.weight(1f))

                Button(
                    modifier = Modifier.width(110.dp),
                    onClick = { vm.regNextStep() },
                    enabled = vm.regCode.isNotBlank() && !vm.isOperating
                ) {
                    Text("下一步")
                }
            }
        } else if (vm.regStep == 2) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = vm.name,
                onValueChange = { vm.name = it.singleLined() },
                label = { Text("昵称") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            )
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = vm.intro,
                onValueChange = { vm.intro = it },
                label = { Text("介绍一下") },
                maxLines = 4,
                minLines = 4,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = vm.gender == 0, onClick = { vm.gender = 0 })
                Text("男")

                RadioButton(selected = vm.gender == 1, onClick = { vm.gender = 1 })
                Text("女")

                RadioButton(selected = vm.gender == 2, onClick = { vm.gender = 2 })
                Text("神没有性别")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { vm.finishRegister() },
                enabled = vm.name.isNotBlank() && vm.intro.isNotBlank() && vm.gender != null && !vm.isOperating
            ) {
                Text("完成")
            }

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { vm.skipProfile() }
            ) {
                Text("跳过")
            }
        }
    }
}