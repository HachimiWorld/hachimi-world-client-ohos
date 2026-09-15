import { common } from "@kit.AbilityKit";
import { ArkUIViewController } from "compose/src/main/cpp/types/libcompose_arkui_utils";

export const MainArkUIViewController: () => ArkUIViewController

export const initPlatform: (openUrl: (url: string) => void, context: common.UIAbilityContext) => void;
export const disposePlatform: () => void;
