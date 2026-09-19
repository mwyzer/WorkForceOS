import path from 'path';

/** Absolute path to a screenshot file in the repository-root `screenshot/` folder. */
export function screenshotPath(file: string): string {
  return path.join(process.cwd(), '..', 'screenshot', file);
}