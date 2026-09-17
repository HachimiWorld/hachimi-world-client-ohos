import { AbilityConstant, ConfigurationConstant, UIAbility, Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';
import { BusinessError } from '@kit.BasicServicesKit';
import { initPlatform, disposePlatform } from 'libentry.so';
import { pasteboard } from '@kit.BasicServicesKit';
import { backgroundTaskManager } from '@kit.BackgroundTasksKit';
import { wantAgent, WantAgent } from '@kit.AbilityKit';

const DOMAIN = 0x0000;
const TRANSPARENT = '#00000000';

export default class EntryAbility extends UIAbility {
  private mainWindow: window.Window | undefined;
  private backgroundPlaybackRunning = false;

  /**
   * Keeps the process alive while audio is playing so playback continues after the app goes to the
   * background. HarmonyOS cancels an `AUDIO_PLAYBACK` task that does not own an AVSession, so this
   * only works together with the media session in Kotlin.
   */
  private async updateBackgroundPlayback(active: boolean): Promise<void> {
    if (active === this.backgroundPlaybackRunning) return;
    try {
      if (active) {
        const notification: WantAgent = await wantAgent.getWantAgent({
          wants: [{
            bundleName: this.context.abilityInfo.bundleName,
            abilityName: this.context.abilityInfo.name
          }],
          operationType: wantAgent.OperationType.START_ABILITY,
          requestCode: 0,
          wantAgentFlags: [wantAgent.WantAgentFlags.UPDATE_PRESENT_FLAG]
        });
        await backgroundTaskManager.startBackgroundRunning(
          this.context, backgroundTaskManager.BackgroundMode.AUDIO_PLAYBACK, notification);
        this.backgroundPlaybackRunning = true;
        hilog.info(DOMAIN, 'HachimiPlatform', 'Background playback started');
      } else {
        await backgroundTaskManager.stopBackgroundRunning(this.context);
        this.backgroundPlaybackRunning = false;
        hilog.info(DOMAIN, 'HachimiPlatform', 'Background playback stopped');
      }
    } catch (error) {
      hilog.error(DOMAIN, 'HachimiPlatform',
        'Background playback update failed: %{public}s', JSON.stringify(error));
    }
  }


  /**
   * Immersive layout: let the window (and therefore Compose) draw underneath the status and
   * navigation bars, and make those bars transparent. Compose then reads the window avoid area
   * and pads its own content, instead of the system reserving opaque bands.
   */
  private applyImmersiveLayout(contentColor: string): void {
    const win = this.mainWindow;
    if (win === undefined) return;
    win.setWindowLayoutFullScreen(true)
      .catch((error: BusinessError) => {
        hilog.error(DOMAIN, 'HachimiPlatform', 'setWindowLayoutFullScreen failed: %{public}s', error.message);
      });
    win.setWindowSystemBarProperties({
      statusBarColor: TRANSPARENT,
      navigationBarColor: TRANSPARENT,
      statusBarContentColor: contentColor,
      navigationBarContentColor: contentColor
    }).catch((error: BusinessError) => {
      hilog.error(DOMAIN, 'HachimiPlatform', 'System bars failed: %{public}s', error.message);
    });
  }

  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    initPlatform((operation: string, value: string): void => {
      if (operation === 'copyText') {
        pasteboard.getSystemPasteboard().setData(pasteboard.createData(pasteboard.MIMETYPE_TEXT_PLAIN, value))
          .catch((error: BusinessError) => hilog.error(DOMAIN, 'HachimiPlatform', 'Clipboard failed: %{public}s', error.message));
        return;
      }
      if (operation === 'systemBars') {
        const color = value === 'true' ? '#FFFFFF' : '#000000';
        this.applyImmersiveLayout(color);
        return;
      }
      if (operation === 'playbackActive') {
        this.updateBackgroundPlayback(value === 'true');
        return;
      }
      if (operation !== 'openUrl') return;
      this.context.startAbility({
        action: 'ohos.want.action.viewData',
        uri: value,
        entities: ['entity.system.browsable']
      }).catch((error: BusinessError) => {
        hilog.error(DOMAIN, 'HachimiPlatform', 'Cannot open URL: %{public}d %{public}s', error.code, error.message);
      });
    }, this.context);
    try {
      this.context.getApplicationContext().setColorMode(ConfigurationConstant.ColorMode.COLOR_MODE_NOT_SET);
    } catch (err) {
      hilog.error(DOMAIN, 'testTag', 'Failed to set colorMode. Cause: %{public}s', JSON.stringify(err));
    }
    hilog.info(DOMAIN, 'testTag', '%{public}s', 'Ability onCreate');
  }

  onDestroy(): void {
    this.updateBackgroundPlayback(false);
    disposePlatform();
    hilog.info(DOMAIN, 'testTag', '%{public}s', 'Ability onDestroy');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    this.mainWindow = windowStage.getMainWindowSync();
    // Enable immersive layout before the content is created, so Compose sees a full-screen window
    // on its first layout pass and reports real safe-area insets.
    this.applyImmersiveLayout('#000000');
    // Main window is created, set main page for this ability
    hilog.info(DOMAIN, 'testTag', '%{public}s', 'Ability onWindowStageCreate');

    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        hilog.error(DOMAIN, 'testTag', 'Failed to load the content. Cause: %{public}s', JSON.stringify(err));
        return;
      }
      hilog.info(DOMAIN, 'testTag', 'Succeeded in loading the content.');
    });
  }

  onWindowStageDestroy(): void {
    // Main window is destroyed, release UI related resources
    hilog.info(DOMAIN, 'testTag', '%{public}s', 'Ability onWindowStageDestroy');
  }

  onForeground(): void {
    // Ability has brought to foreground
    hilog.info(DOMAIN, 'testTag', '%{public}s', 'Ability onForeground');
  }

  onBackground(): void {
    // Ability has back to background
    hilog.info(DOMAIN, 'testTag', '%{public}s', 'Ability onBackground');
  }
}
