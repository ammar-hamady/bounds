import { useEffect, useState, type ComponentType } from 'react';

import { modules as discoveredModules } from './.generated/mockup-components';

type ModuleMap = Record<string, () => Promise<Record<string, unknown>>>;

function _resolveComponent(
  mod: Record<string, unknown>,
  name: string,
): ComponentType | undefined {
  const fns = Object.values(mod).filter(
    (v) => typeof v === 'function',
  ) as ComponentType[];
  return (
    (mod.default as ComponentType) ||
    (mod.Preview as ComponentType) ||
    (mod[name] as ComponentType) ||
    fns[fns.length - 1]
  );
}

function PreviewRenderer({
  componentPath,
  modules,
}: {
  componentPath: string;
  modules: ModuleMap;
}) {
  const [Component, setComponent] = useState<ComponentType | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    setComponent(null);
    setError(null);

    async function loadComponent(): Promise<void> {
      const key = `./components/mockups/${componentPath}.tsx`;
      const loader = modules[key];
      if (!loader) {
        setError(`No component found at ${componentPath}.tsx`);
        return;
      }

      try {
        const mod = await loader();
        if (cancelled) {
          return;
        }
        const name = componentPath.split('/').pop()!;
        const comp = _resolveComponent(mod, name);
        if (!comp) {
          setError(
            `No exported React component found in ${componentPath}.tsx\n\nMake sure the file has at least one exported function component.`,
          );
          return;
        }
        setComponent(() => comp);
      } catch (e) {
        if (cancelled) {
          return;
        }

        const message = e instanceof Error ? e.message : String(e);
        setError(`Failed to load preview.\n${message}`);
      }
    }

    void loadComponent();

    return () => {
      cancelled = true;
    };
  }, [componentPath, modules]);

  if (error) {
    return (
      <pre style={{ color: 'red', padding: '2rem', fontFamily: 'system-ui' }}>
        {error}
      </pre>
    );
  }

  if (!Component) return null;

  return <Component />;
}

function getBasePath(): string {
  return import.meta.env.BASE_URL.replace(/\/$/, '');
}

function getPreviewExamplePath(): string {
  const basePath = getBasePath();
  return `${basePath}/preview/ComponentName`;
}

type PreviewEntry = {
  componentPath: string;
  name: string;
  group: string;
};

function getPreviewEntries(): PreviewEntry[] {
  return Object.keys(discoveredModules)
    .map((moduleKey) => {
      const componentPath = moduleKey
        .replace(/^\.\/components\/mockups\//, '')
        .replace(/\.tsx$/, '');
      const parts = componentPath.split('/');
      const name = parts.pop() ?? componentPath;

      return {
        componentPath,
        name,
        group: parts.join(' / ') || 'Components',
      };
    })
    .sort((a, b) =>
      `${a.group}/${a.name}`.localeCompare(`${b.group}/${b.name}`),
    );
}

function Gallery() {
  const basePath = getBasePath();
  const previews = getPreviewEntries();
  const groups = previews.reduce<Record<string, PreviewEntry[]>>(
    (result, preview) => {
      (result[preview.group] ??= []).push(preview);
      return result;
    },
    {},
  );

  return (
    <div className="min-h-screen bg-slate-950 px-6 py-12 text-slate-100">
      <main className="mx-auto max-w-4xl">
        <div className="mb-10">
          <p className="mb-3 text-xs font-semibold uppercase tracking-[0.2em] text-amber-400">
            Bounds UI
          </p>
          <h1 className="text-3xl font-semibold tracking-tight">
            Component Preview Server
          </h1>
          <p className="mt-3 max-w-xl text-sm leading-6 text-slate-400">
            Open a component below to view it in the browser. These previews
            are generated automatically from the mockup component registry.
          </p>
        </div>

        {Object.entries(groups).map(([group, groupPreviews]) => (
          <section key={group} className="mb-8">
            <h2 className="mb-3 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
              {group}
            </h2>
            <div className="grid gap-3 sm:grid-cols-2">
              {groupPreviews.map((preview) => (
                <a
                  key={preview.componentPath}
                  href={`${basePath}/preview/${preview.componentPath}`}
                  className="group rounded-xl border border-slate-800 bg-slate-900/80 p-5 transition-colors hover:border-amber-400/60 hover:bg-slate-900"
                >
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <h3 className="font-medium text-slate-100 transition-colors group-hover:text-amber-300">
                        {preview.name}
                      </h3>
                      <p className="mt-1 text-xs text-slate-500">
                        {preview.componentPath}
                      </p>
                    </div>
                    <span
                      aria-hidden="true"
                      className="text-lg text-slate-600 transition-colors group-hover:text-amber-400"
                    >
                      →
                    </span>
                  </div>
                </a>
              ))}
            </div>
          </section>
        ))}

        {previews.length === 0 && (
          <p className="rounded-xl border border-dashed border-slate-800 p-8 text-sm text-slate-500">
            No preview components have been discovered yet.
          </p>
        )}
      </main>
    </div>
  );
}

function getPreviewPath(): string | null {
  const basePath = getBasePath();
  const { pathname } = window.location;
  const local =
    basePath && pathname.startsWith(basePath)
      ? pathname.slice(basePath.length) || '/'
      : pathname;
  const match = local.match(/^\/preview\/(.+)$/);
  return match ? match[1] : null;
}

function App() {
  const previewPath = getPreviewPath();

  if (previewPath) {
    return (
      <PreviewRenderer
        componentPath={previewPath}
        modules={discoveredModules}
      />
    );
  }

  return <Gallery />;
}

export default App;
