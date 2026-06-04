interface SportTypeTabsProps {
  sportTypes: { sportTypeId: number, sportName: string }[]
  selectedId: number | undefined
  onSelect: (id: number | undefined) => void
}

export function SportTypeTabs({ sportTypes, selectedId, onSelect }: SportTypeTabsProps) {
  return (
    <div className="w-full max-w-7xl mx-auto px-4 mb-8">
      <div className="flex gap-3 overflow-x-auto pb-4 scrollbar-hide snap-x">
        <button
          onClick={() => onSelect(undefined)}
          className={`snap-start shrink-0 px-6 py-3 rounded-full font-semibold text-sm transition-all shadow-sm
            ${!selectedId
              ? 'bg-gray-900 text-white dark:bg-white dark:text-gray-900 shadow-lg scale-105'
              : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-200 dark:bg-card dark:text-gray-400 dark:border-border'
            }
          `}
        >
          Tất cả môn
        </button>

        {sportTypes.map((st) => {
          const isSelected = selectedId === st.sportTypeId
          return (
            <button
              key={st.sportTypeId}
              onClick={() => onSelect(st.sportTypeId)}
              className={`snap-start shrink-0 px-6 py-3 rounded-full font-semibold text-sm transition-all shadow-sm flex items-center
                ${isSelected
                  ? 'bg-gray-900 text-white dark:bg-white dark:text-gray-900 shadow-lg scale-105'
                  : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-200 dark:bg-card dark:text-gray-400 dark:border-border'
                }
              `}
            >
              {st.sportName}
            </button>
          )
        })}
      </div>
    </div>
  )
}
